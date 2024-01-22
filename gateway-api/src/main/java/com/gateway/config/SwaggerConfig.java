//package com.gateway.config;
//
//import java.util.ArrayList;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.config.annotation.EnableWebMvc;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import springfox.documentation.builders.ApiInfoBuilder;
//import springfox.documentation.builders.ParameterBuilder;
//import springfox.documentation.builders.PathSelectors;
//import springfox.documentation.builders.RequestHandlerSelectors;
//import springfox.documentation.schema.ModelRef;
//import springfox.documentation.service.ApiInfo;
//import springfox.documentation.service.Contact;
//import springfox.documentation.service.Parameter;
//import springfox.documentation.spi.DocumentationType;
//import springfox.documentation.spring.web.plugins.Docket;
//import springfox.documentation.swagger2.annotations.EnableSwagger2;
//
//@Configuration
////@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = false)
//@EnableWebMvc
//
//@Component
//public class SwaggerConfig implements WebMvcConfigurer {
//
//	
//	 @Override
//	    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//	        registry.addResourceHandler("/swagger-ui/**")
//	                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
//	                .resourceChain(false);
//	    }
//		@Bean
//		public Docket api() {
//			ParameterBuilder aParameterBuilder = new ParameterBuilder();
//			aParameterBuilder.name("X-Auth-Token").modelRef(new ModelRef("string")).parameterType("header")
//					.required(false).build();
//
//			List<Parameter> aParameters = new ArrayList<Parameter>();
//			aParameters.add(aParameterBuilder.build());
//
//			return new Docket(DocumentationType.SWAGGER_2).apiInfo(metadata()).select()
//					.apis(RequestHandlerSelectors.any()).paths(PathSelectors.any()).build()
//					.globalOperationParameters(aParameters);
//
//		}
//
//		private ApiInfo metadata() {
//			return new ApiInfoBuilder().title("NBP-BISP").description(
//					"Services for Functional Specs for BISP API Integration Social Protection Account Payment API")
//					.version("version")
//					.contact(new Contact("Paysys Labs", "http://www.paysyslabs.com/", "info@paysyslabs.com")).build();
//		}
//
//	}
//
