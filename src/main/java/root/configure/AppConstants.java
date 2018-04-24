package root.configure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file")
public class AppConstants {
    private String reportPath;//文件服务路径
    private String staticReportPath;//静态报表路径
    private String dynamicReportPath;//动态报表路径
    private String excelFilePath;
    private String mobileSubjectPath;//手机主题路径
    private String templatePath;//报表Excel模板路径
    private String fillTemplatePath;
    private String userSqlPath;//SQL模板文件服务路径
    private String userFunctionPath;
    private String userDictionaryPath;
    private String webServicePath;
    private String mongoTemplate;//mongdb元数据保存路径
    private String appFilePath;
    private String LambdaUrl;//LambdaAddress计算地址
    private String clientInstallFile;
    private String report2;//前端路径

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getStaticReportPath() {
        return staticReportPath;
    }

    public void setStaticReportPath(String staticReportPath) {
        this.staticReportPath = staticReportPath;
    }

    public String getDynamicReportPath() {
        return dynamicReportPath;
    }

    public void setDynamicReportPath(String dynamicReportPath) {
        this.dynamicReportPath = dynamicReportPath;
    }

    public String getExcelFilePath() {
        return excelFilePath;
    }

    public void setExcelFilePath(String excelFilePath) {
        this.excelFilePath = excelFilePath;
    }

    public String getAppFilePath() {
        return appFilePath;
    }

    public void setAppFilePath(String appFilePath) {
        this.appFilePath = appFilePath;
    }

    public String getMobileSubjectPath() {
        return mobileSubjectPath;
    }

    public void setMobileSubjectPath(String mobileSubjectPath) {
        this.mobileSubjectPath = mobileSubjectPath;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getFillTemplatePath() {
        return fillTemplatePath;
    }

    public void setFillTemplatePath(String fillTemplatePath) {
        this.fillTemplatePath = fillTemplatePath;
    }

    public String getUserSqlPath() {
        return userSqlPath;
    }

    public void setUserSqlPath(String userSqlPath) {
        this.userSqlPath = userSqlPath;
    }

    public String getUserFunctionPath() {
        return userFunctionPath;
    }

    public void setUserFunctionPath(String userFunctionPath) {
        this.userFunctionPath = userFunctionPath;
    }

    public String getUserDictionaryPath() {
        return userDictionaryPath;
    }

    public void setUserDictionaryPath(String userDictionaryPath) {
        this.userDictionaryPath = userDictionaryPath;
    }

    public String getWebServicePath() {
        return webServicePath;
    }

    public void setWebServicePath(String webServicePath) {
        this.webServicePath = webServicePath;
    }

    public String getMongoTemplate() {
        return mongoTemplate;
    }

    public void setMongoTemplate(String mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String getLambdaUrl() {
        return LambdaUrl;
    }

    public void setLambdaUrl(String lambdaUrl) {
        LambdaUrl = lambdaUrl;
    }

    public String getClientInstallFile() {
        return clientInstallFile;
    }

    public void setClientInstallFile(String clientInstallFile) {
        this.clientInstallFile = clientInstallFile;
    }

    public String getReport2() {
        return report2;
    }

    public void setReport2(String report2) {
        this.report2 = report2;
    }

    @Override
    public String toString() {
        return "AppConstants{" +
                "reportPath='" + reportPath + '\'' +
                ", staticReportPath='" + staticReportPath + '\'' +
                ", dynamicReportPath='" + dynamicReportPath + '\'' +
                ", excelFilePath='" + excelFilePath + '\'' +
                ", mobileSubjectPath='" + mobileSubjectPath + '\'' +
                ", templatePath='" + templatePath + '\'' +
                ", fillTemplatePath='" + fillTemplatePath + '\'' +
                ", userSqlPath='" + userSqlPath + '\'' +
                ", userFunctionPath='" + userFunctionPath + '\'' +
                ", userDictionaryPath='" + userDictionaryPath + '\'' +
                ", webServicePath='" + webServicePath + '\'' +
                ", mongoTemplate='" + mongoTemplate + '\'' +
                ", appFilePath='" + appFilePath + '\'' +
                ", LambdaUrl='" + LambdaUrl + '\'' +
                '}';
    }
}
