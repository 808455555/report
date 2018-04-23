package pccw.wj.common;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

public interface IControl {
	@ResponseBody String getInputOutputParas(@RequestBody String pJson);
	
	@ResponseBody String saveMetaData(@RequestBody String pJson);
}
