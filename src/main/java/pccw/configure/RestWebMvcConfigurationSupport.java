package pccw.configure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import pccw.wj.interceptor.RestInterceptor;

import java.io.File;

@Configuration
public class RestWebMvcConfigurationSupport extends WebMvcConfigurationSupport {
//    @Override
//    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        super.configureMessageConverters(converters);
//        FastJsonHttpMessageConverter4 fastConverter = new FastJsonHttpMessageConverter4();
//        FastJsonConfig fastConfig =  new FastJsonConfig();
//        fastConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
//        fastConverter.setFastJsonConfig(fastConfig);
//        converters.add(fastConverter);
//    }
    @Autowired
    private AppConstants appConstants;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println(appConstants);
        registry.addResourceHandler("/report/static/**").addResourceLocations("file:"+appConstants.getStaticReportPath()+ File.separator);
        registry.addResourceHandler("/report/dynamic/**").addResourceLocations("file:"+appConstants.getDynamicReportPath()+File.separator);
        registry.addResourceHandler("/ibas2/**").addResourceLocations("file:"+appConstants.getClientInstallFile()+File.separator);
        registry.addResourceHandler("/**").addResourceLocations("file:"+appConstants.getReport2()+File.separator);
    }

    @Override
    protected void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/reportServer/**")
                .allowedOrigins("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT")
                .maxAge(36000);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] EXCLUDE_URL = {"/reportServer/fonts/*","/reportServer/css/*","/reportServer/js/*",
                                "/reportServer/DBConnection/test","/reportServer/DBConnection/save",
                                "/reportServer/user/encodePwd","/reportServer/user/login"};
        registry.addInterceptor(new RestInterceptor())
                .addPathPatterns("/reportServer/**")
                .excludePathPatterns(EXCLUDE_URL);
    }
}
