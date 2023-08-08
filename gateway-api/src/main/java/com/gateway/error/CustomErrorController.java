package com.gateway.error;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.gateway.response.GenericResponse;

import io.swagger.annotations.Api;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Api(tags = "Custom Error Controller")
@RestController
public class CustomErrorController implements ErrorController {

    public static final String PATH = "/error";

    @Value("${debug}")
    private boolean debug;

    @Autowired
    private ErrorAttributes errorAttributes;

    /**
     * Explicitly sending status codes as 200 as clients have a hard time parsing non-200 responses
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = PATH, produces = { MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<GenericResponse<ErrorJson>> error(WebRequest request, HttpServletResponse response) {
        ErrorJson error = new ErrorJson(response.getStatus(), getErrorAttributes(request, debug));
        
        return new ResponseEntity<GenericResponse<ErrorJson>>(
                new GenericResponse<ErrorJson>("99", error.message, error),
                HttpStatus.OK
            );
    }
    
    @Override
    public String getErrorPath() {
        return PATH;
    }

    private Map<String, Object> getErrorAttributes(WebRequest request, boolean includeStackTrace) {
        return errorAttributes.getErrorAttributes(request, includeStackTrace);
    }
}
