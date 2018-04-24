package root.report.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import root.configure.AppConstants;
import root.configure.WebApplicationContext;
import root.report.util.ErpUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class DbFactory {
	private static final Logger log = Logger.getLogger(DbManager.class);
    public static final String SYSTEM = "system";
    public static final String FORM = "form";
    public static final String BUDGET = "budget";
    private static Map<String,SqlSessionFactory> mapFactory = new HashMap<String,SqlSessionFactory>();
    private static Map<String,ThreadLocal<SqlSession>> map = new HashMap<String,ThreadLocal<SqlSession>>();
    private static ErpUtil erpUtil = null;
    private static DbManager manager = new DbManager();

    static{
        erpUtil = new ErpUtil();
        JSONArray dbs = JSON.parseArray(manager.getAllDBConnections());
        JSONObject obj = null;
        for (int i = 0; i < dbs.size(); i++){
            obj = dbs.getJSONObject(i);
            String dbtype = obj.getString("dbtype");
            if(!"Mysql".equals(dbtype)&&!"Oracle".equals(dbtype)&&!"DB2".equals(dbtype)){
                continue;
            }
            DbFactory.initializeDB(obj.getString("name"));
        }
    }
    // 初始化
    public static void init(String dbName){
    	long t1 = System.nanoTime();
    	try{
	    	JSONObject dbJson = JSONObject.parseObject(manager.getDBConnectionByName(dbName));
	    	if(dbJson.size()==0){
	    		return;
	    	}
	    	String dbtype = dbJson.getString("dbtype");
	    	if(!"Mysql".equals(dbtype)&&!"Oracle".equals(dbtype)&&!"DB2".equals(dbtype)){
	            return;
	        }
	    	SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
	    	DruidDataSource dataSource = new DruidDataSource();
	    	dataSource.setUsername(dbJson.getString("username"));
	        dataSource.setPassword(erpUtil.decode(dbJson.getString("password")));
	    	dataSource.setDriverClassName(dbJson.getString("driver"));
	    	if("Mysql".equals(dbtype)){
	    		dataSource.setUrl(dbJson.getString("url")+"?serverTimezone=UTC&useSSL=true&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&rewriteBatchedStatements=true");	
	    	}
	    	else{
	    		dataSource.setUrl(dbJson.getString("url"));
	    	}
	    	dataSource.setMaxWait(10000);//设置连接超时时间10秒
	        dataSource.setMaxActive(Integer.valueOf(dbJson.getString("maxPoolSize")));
	        dataSource.setInitialSize(Integer.valueOf(dbJson.getString("minPoolSize")));
	        dataSource.setTimeBetweenEvictionRunsMillis(60000);//检测数据源空连接间隔时间
	        dataSource.setMinEvictableIdleTimeMillis(300000);//连接空闲时间
	        dataSource.setTestWhileIdle(true);
	        dataSource.setTestOnBorrow(true);
	        if("Oracle".equals(dbtype)){
	        	dataSource.setPoolPreparedStatements(true);
	        }
	        if("DB2".equals(dbtype)){
	        	dataSource.setValidationQuery("select 'x' from sysibm.sysdummy1");
            }else{
            	dataSource.setValidationQuery("select 'x' from dual");
            }
	        dataSource.setFilters("stat");
//	        List<Filter> filters = new ArrayList<Filter>();
//	        filters.add(new SqlFilter());
//	        dataSource.setProxyFilters(filters);
	        dataSource.init();
	        //填充数据源
	        factoryBean.setDataSource(dataSource);
	        //填充SQL文件
	        factoryBean.setMapperLocations(getMapLocations(dbtype,dbName));
	        Configuration configuration = new Configuration();
	        configuration.setCallSettersOnNulls(true);
	        //启动SQL日志
	        configuration.setLogImpl(Log4jImpl.class);
	        factoryBean.setConfiguration(configuration);
	        factoryBean.setPlugins(getMybatisPlugins(dbtype));
	        mapFactory.put(dbJson.getString("name"), factoryBean.getObject());
	        long t2 = System.nanoTime();
	        log.info("初始化数据库【"+dbName+"】耗时"+ String.format("%.4fs", (t2 - t1) * 1e-9));
    	}catch(Exception e){
    		log.error("初始化数据库【"+dbName+"】失败!");
    		e.printStackTrace();
    	}
    }
    public static SqlSession Open(String dbName) {
        return Open(true, dbName);
    }

    // 获取一个session
    public static SqlSession Open(boolean autoCommit, String dbName){
        ThreadLocal<SqlSession> mybatisTl = map.get(dbName);
        if(mybatisTl==null){
            SqlSessionFactory factory = mapFactory.get(dbName);
            if(factory==null){
                init(dbName);
            }
            mybatisTl = new ThreadLocal<SqlSession>();
            mybatisTl.set(mapFactory.get(dbName).openSession(autoCommit));
            map.put(dbName, mybatisTl);
        }else{
            if(mybatisTl.get()==null){
                mybatisTl.set(mapFactory.get(dbName).openSession(autoCommit));
            }
        }
        
        return mybatisTl.get();
    }

    // 关闭session
    public static void close(String dbName) {
        ThreadLocal<SqlSession> mybatisTl = map.get(dbName);
        if(mybatisTl!=null){
            SqlSession session =  mybatisTl.get();
            if (session != null) {
                session.close();
                mybatisTl.set(null);
            }
        }
    }

    // 回滚
    public static void rollback(String dbName) {
        ThreadLocal<SqlSession> mybatisTl = map.get(dbName);
        if(mybatisTl!=null){
            SqlSession session = mybatisTl.get();
            if (session != null) {
                session.rollback();
            } 
        }
    }

    // 提交
    public static void commit(String dbName) {
        ThreadLocal<SqlSession> mybatisTl = map.get(dbName);
        if(mybatisTl!=null){
            SqlSession session = mybatisTl.get();
            if (session != null) {
                session.commit();
            }
        }
    }
    
    private static Resource[] getMapLocations(String dbType,String dbName){
    	String[] locations = new String[3];
    	if(DbFactory.SYSTEM.equals(dbName)){
            locations = new String[4];
            locations[3] = DbFactory.class.getClassLoader().getResource("oracleSql").getPath();
        }else if(DbFactory.FORM.equals(dbName)){
            locations = new String[4];
            locations[3] = DbFactory.class.getClassLoader().getResource("mySqlSql").getPath();
        }else if(DbFactory.BUDGET.equals(dbName)){
    		locations = new String[4];
    		locations[3] = DbFactory.class.getClassLoader().getResource("db2Sql").getPath();
    	}
        AppConstants appConstants = WebApplicationContext.getBean(AppConstants.class);
        locations[0] = appConstants.getUserSqlPath();
        locations[1] = appConstants.getUserFunctionPath();
        locations[2] = appConstants.getUserDictionaryPath();
    	List<FileSystemResource> resources = new ArrayList<FileSystemResource>();
    	for (String location:locations){
    		File[] fileList = new File(location).listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					if (name.endsWith(".xml")) {
						return true;
					}
					return false;
				}});
    		for(int i = 0;fileList!=null&&i<fileList.length;i++){
    			resources.add(new FileSystemResource(fileList[i])); 
    		}
		}
    	FileSystemResource[] a = new FileSystemResource[resources.size()];
    	return resources.toArray(a);
    }
    
    private static Interceptor[] getMybatisPlugins(String dbType){
    	Interceptor[] ins = new Interceptor[1];
    	PageInterceptor pageInceptor = new PageInterceptor();
    	Properties pro = new Properties();
    	if("Oracle".equals(dbType)){
    		pro.setProperty("helperDialect", "oracle");
    	}else if("Mysql".equals(dbType)){
    		pro.setProperty("helperDialect", "mysql");
    	}else if("DB2".equals(dbType)){
    		pro.setProperty("helperDialect", "db2");
    	}
    	pro.setProperty("rowBoundsWithCount", "true");
    	pageInceptor.setProperties(pro);
    	ins[0] = pageInceptor;
    	return ins;
    }
    
    public static void initializeDB(String dbName){
        map.remove(dbName);
        mapFactory.remove(dbName);
        init(dbName);
    }
}
