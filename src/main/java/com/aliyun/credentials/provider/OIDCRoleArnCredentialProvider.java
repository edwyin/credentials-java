package com.aliyun.credentials.provider;


import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.OIDCRoleArnCredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.*;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.StringUtils;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OIDCRoleArnCredentialProvider implements AlibabaCloudCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds = 3600;
    /**
     * The arn of the role to be assumed.
     */
    private String roleArn;
    private String oidcProviderArn;

    private String oidcToken;
    private String oidcTokenFilePath;

    /**
     * An identifier for the assumed role session.
     */
    private String roleSessionName = "defaultSessionName";

    private String accessKeyId;
    private String accessKeySecret;
    private String regionId = "cn-hangzhou";
    private String policy;

    /**
     * Unit of millisecond
     */
    private int connectTimeout = 1000;
    private int readTimeout = 1000;

    public OIDCRoleArnCredentialProvider(Configuration config) {
        this(config.getAccessKeyId(), config.getAccessKeySecret(), config.getRoleArn(),
                config.getOIDCProviderArn(), config.getOIDCTokenFilePath());
        this.roleSessionName = config.getRoleSessionName();
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
    }

    public OIDCRoleArnCredentialProvider(Config config) {
        this(config.accessKeyId, config.accessKeySecret, config.roleArn, config.oidcProviderArn, config.oidcTokenFilePath);
        this.roleSessionName = config.roleSessionName;
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
        this.policy = config.policy;
        this.durationSeconds = config.roleSessionExpiration;
    }

    public OIDCRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleArn,
                                         String oidcProviderArn, String oidcTokenFilePath) {
        this.roleArn = roleArn;
        this.oidcProviderArn = oidcProviderArn;
        if (!StringUtils.isEmpty(oidcTokenFilePath)) {
            this.oidcTokenFilePath = oidcTokenFilePath;
        } else {
            String tokenFile = System.getenv("ALIBABA_CLOUD_OIDC_TOKEN_FILE");
            if (StringUtils.isEmpty(tokenFile)) {
                throw new CredentialException("OIDCTokenFilePath does not exist and env ALIBABA_CLOUD_OIDC_TOKEN_FILE is null.");
            }
            this.oidcTokenFilePath = tokenFile;
        }
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    public OIDCRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleSessionName,
                                         String roleArn, String oidcProviderArn, String oidcTokenFilePath,
                                         String regionId, String policy) {
        this(accessKeyId, accessKeySecret, roleArn, oidcProviderArn, oidcTokenFilePath);
        this.roleSessionName = roleSessionName;
        this.regionId = regionId;
        this.policy = policy;
    }


    @Override
    public AlibabaCloudCredentials getCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        return createCredential(client);
    }

    public AlibabaCloudCredentials createCredential(CompatibleUrlConnClient client) {
        try {
            return getNewSessionCredentials(client);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public AlibabaCloudCredentials getNewSessionCredentials(CompatibleUrlConnClient client) throws UnsupportedEncodingException {
        this.oidcToken = AuthUtils.getOIDCToken(oidcTokenFilePath);
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "AssumeRoleWithOIDC");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("RegionId", this.regionId);
        Map<String, String> body = new HashMap<String, String>();
        body.put("DurationSeconds", String.valueOf(durationSeconds));
        body.put("RoleArn", this.roleArn);
        body.put("OIDCProviderArn", this.oidcProviderArn);
        body.put("OIDCToken", this.oidcToken);
        body.put("RoleSessionName", this.roleSessionName);
        body.put("Policy", this.policy);
        StringBuilder content = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                content.append("&");
            }
            content.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            content.append("=");
            content.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        httpRequest.setHttpContent(content.toString().getBytes("UTF-8"),"UTF-8", FormatType.FORM);
        if (!StringUtils.isEmpty(this.accessKeyId) && !StringUtils.isEmpty(this.accessKeySecret)) {
            httpRequest.setUrlParameter("AccessKeyId", this.accessKeyId);
            Map<String, String> paramsToSign = new HashMap<String, String>();
            paramsToSign.putAll(httpRequest.getUrlParameters());
            paramsToSign.putAll(body);
            String strToSign = parameterHelper.composeStringToSign(MethodType.POST, paramsToSign);
            String signature = parameterHelper.signString(strToSign, this.accessKeySecret + "&");
            httpRequest.setUrlParameter("Signature", signature);
        }
        httpRequest.setSysMethod(MethodType.POST);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.setSysUrl(parameterHelper.composeUrl("sts.aliyuncs.com", httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse = client.syncInvoke(httpRequest);
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (map.containsKey("Credentials")) {
            Map<String, String> credential = (Map<String, String>) map.get("Credentials");
            long expiration = ParameterHelper.getUTCDate(credential.get("Expiration")).getTime();
            return new OIDCRoleArnCredential(credential.get("AccessKeyId"), credential.get("AccessKeySecret"),
                    credential.get("SecurityToken"), expiration, this);
        } else {
            throw new CredentialException(gson.toJson(map));
        }
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public String getOIDCProviderArn() {
        return oidcProviderArn;
    }

    public String getOIDCToken() {
        return oidcToken;
    }

    public String getOIDCTokenFilePath() {
        return oidcTokenFilePath;
    }

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}