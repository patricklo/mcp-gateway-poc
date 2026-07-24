package cn.bugstack.ai.mcp.server.test.service;

import cn.bugstack.ai.mcp.server.test.model.XxxRequest01;
import cn.bugstack.ai.mcp.server.test.model.XxxRequest02;
import cn.bugstack.ai.mcp.server.test.model.XxxResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class TestService {

    @Tool(description = "获取公司雇员信息")
    public XxxResponse getCompanyEmployee(XxxRequest01 xxxRequest01) {
        log.info("根据公司和雇员，查询工资和工作工时。{}",xxxRequest01.getCompany());

        // 这部分可以实际调用你需要的接口，比如调用http接口获取个数据或者做一些操作等。

        XxxResponse xxxResponse = new XxxResponse();
        xxxResponse.setSalary("16.92");
        xxxResponse.setDayManHour(String.valueOf(new Random().nextInt(24)));

        XxxResponse.User user = new XxxResponse.User();
        user.setUserId("111");
        user.setUserName("小傅哥");

        xxxResponse.setUser(user);

        XxxResponse xxxResponse02 = new XxxResponse();
        xxxResponse02.setSalary("16.92");
        xxxResponse02.setDayManHour(String.valueOf(new Random().nextInt(24)));

        XxxResponse.User user02 = new XxxResponse.User();
        user02.setUserId("111");
        user02.setUserName("小傅哥");

        xxxResponse02.setUser(user);

        List<XxxResponse> xlist = new ArrayList<>();
        xlist.add(xxxResponse);
        xlist.add(xxxResponse02);

        return xxxResponse;
    }

    @Tool(description = "查询客户端信息")
    public XxxResponse queryAiClientById(Long id) {
        XxxResponse xxxResponse = new XxxResponse();
        xxxResponse.setSalary("16.92");
        xxxResponse.setDayManHour(String.valueOf(new Random().nextInt(24)));

        XxxResponse.User user = new XxxResponse.User();
        user.setUserId("111");
        user.setUserName("小傅哥");

        xxxResponse.setUser(user);

        return xxxResponse;
    }

}
