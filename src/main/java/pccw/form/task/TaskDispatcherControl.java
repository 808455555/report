package pccw.form.task;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportServer")
public class TaskDispatcherControl {
	
	
	//******************以下为任务分配人API***********************
	
    //创建填报任务
	@RequestMapping(value = "/create", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String create() {
	
		//创建填报任务，写任务主表，写任务从表
		
		return "";
		
	}
	
	 //查看创建的填报任务
	@RequestMapping(value = "/getDispatcherTask", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getDispatcherTask() {
	
		//创建填报任务
		
		
		return "";
		
	}

}
