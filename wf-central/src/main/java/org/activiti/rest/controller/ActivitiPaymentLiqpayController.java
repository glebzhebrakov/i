package org.activiti.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wf.dp.dniprorada.util.Util;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;

@Api(tags = { "ActivitiPaymentLiqpayController" }, description = "ActivitiPaymentLiqpayController")
@Controller
public class ActivitiPaymentLiqpayController {

    private final Logger LOG = LoggerFactory.getLogger(ActivitiPaymentLiqpayController.class);
    private StringBuffer sb = new StringBuffer();

    @ApiOperation(value = "/setPaymentNewStatus_Liqpay", notes = "нет описания" )
    @RequestMapping(value = "/setPaymentNewStatus_Liqpay", method = RequestMethod.GET, headers = {
            "Accept=application/json" })
    public
    @ResponseBody
    String setPaymentNewStatus_Liqpay(
	    @ApiParam(value = "нет описания", required = true) @RequestParam String sID_Order,
	    @ApiParam(value = "нет описания", required = true) @RequestParam String sHost) {
        sb.append(sHost);
        String data = "data"; // вместо "data" подставить ответ вызова API
        String t = "";            // liqpay
        sb.append("sID_Order=");
        sb.append(sID_Order);
        sb.append("&sData=");
        sb.append(data);
        sb.append("&sID_PaymentSystem=Liqpay");
        try {
            if (sID_Order.startsWith("TaskActiviti_")) {
                t = setPaymentStatus_TaskActiviti(sHost, sb.toString(), data);
            }
        } catch (Exception e) {
            LOG.error("HttpAnswer error:", e);
        }
        return t + "/";
    }

    private String setPaymentStatus_TaskActiviti(String sHost, String url, String sData) throws Exception {
        return Util.httpAnswer(sb.toString(), sData);
    }
}
