package pccw.wj.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Servlet Filter implementation class RestFilter
 */
@WebFilter("/reportServer/*")
public class BodyFilter implements Filter {

    public BodyFilter() {}

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String reqMethod = httpRequest.getMethod();
        if("POST".equals(reqMethod)){
        	BodyHttpServletRequestWrapper bodyRequestWrapper= new BodyHttpServletRequestWrapper(httpRequest);
    		chain.doFilter(bodyRequestWrapper, response);
        }else{
            chain.doFilter(request, response);
        }
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {}
}
