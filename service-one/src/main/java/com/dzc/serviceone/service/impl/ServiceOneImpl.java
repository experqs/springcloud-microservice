package com.dzc.serviceone.service.impl;

import com.dzc.common.model.Result;
import com.dzc.common.util.RestTemplateUtil;
import com.dzc.common.util.ResultUtil;
import com.dzc.serviceone.service.ServiceOne;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ServiceOneImpl implements ServiceOne {

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 使用RestTemplate封装工具，消费【外部服务API】
     */
    @Override
    public Result getStock(String stock) {
        String url = "http://qt.gtimg.cn/";
        Map<String, String> paramsMap = new HashMap<>();
        if (StringUtils.isEmpty(stock)) {
            // 股票代码为空则获取沪指
            paramsMap.put("q", "sh000001");
        } else {
            paramsMap.put("q", stock);
        }
        // 使用封装过的restTemplate工具调用外部API
        ResponseEntity<String> response = restTemplateUtil.get(url, paramsMap, String.class, null);
        return ResultUtil.success(response.getBody());
    }

    /**
     * 使用 RestTemplate + Ribbon，消费【内部服务API】
     *
     * 此restTemplate实例使用@LoadBalanced注解开启了Ribbon的负载均衡功能，要使得负载均衡有效就不能直接指定目标服务的具体url地址（IP+端口号）
     * 这里用的服务名替代了具体的url地址，Ribbon会根据服务名来选择具体的服务实例（若实例有多个，则轮询使用，实现负载均衡），并使用具体的url替换掉服务名
     * （如果不启用@LoadBalanced，则无法把服务名替换为url地址）
     *
     * @HystrixCommand 指定了当消费内部、外部服务过程中因超时等原因而出错时，回退所用的fallback方法
     *     （需在启动类加注解 @EnableHystrix 来激活熔断降级功能）
     */
    @Override
    @HystrixCommand(fallbackMethod = "helloServiceTwoByHystrix")
    public Result helloServiceTwoRestTemplate(Integer id) {
        String url = "http://service-two/hello";
        if (id != null) {
            String params = "?id=" + id;
            url = url + params;
        }
        // 使用原生的restTemplate调用内部API
        String response = restTemplate.getForObject(url, String.class);
        return ResultUtil.success(response);
    }

    /**
     * 当消费内部、外部服务过程中因超时等原因而出错时，回退所用的fallback方法
     * （该方法的 入参、返回值类型 需与原方法一致；但方法的作用范围如public、private，则允许不同）
     * @param id
     * @return
     */
    private Result helloServiceTwoByHystrix(Integer id) {
        System.out.println("RestTemplate消费ServiceTwo出错，启动熔断降级！");
        return ResultUtil.fail("RestTemplate在消费ServiceTwo时出现熔断，这是来自Hystrix的降级处理！");
    }
}
