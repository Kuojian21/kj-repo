import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.beust.jcommander.internal.Lists;
public class Solution {
	
	public static void main(String[] args){
		Pattern pattern = Pattern.compile("Ep(\\d{2}).*(.mp4)");
		for(File file:new File("E:\\迅雷下载\\琅琊榜").listFiles()){
			String name = file.getName();
			Matcher matcher = pattern.matcher(file.getName());
			if(matcher.find()){
				System.out.println(matcher.group(1)+matcher.group(2));
				file.renameTo(new File("E:\\迅雷下载\\琅琊榜\\琅琊榜EP" + matcher.group(1)+matcher.group(2)));
			}
			
		}
	}
	
}