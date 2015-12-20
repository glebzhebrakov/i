package org.activiti.rest.controller;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.wf.dp.dniprorada.base.dao.EscalationHistoryDao;
import org.wf.dp.dniprorada.base.dao.EscalationRuleDao;
import org.wf.dp.dniprorada.base.dao.EscalationRuleFunctionDao;
import org.wf.dp.dniprorada.base.dao.EscalationStatusDao;
import org.wf.dp.dniprorada.base.model.EscalationHistory;
import org.wf.dp.dniprorada.base.model.EscalationRule;
import org.wf.dp.dniprorada.base.model.EscalationRuleFunction;
import org.wf.dp.dniprorada.base.model.EscalationStatus;
import org.wf.dp.dniprorada.base.service.escalation.EscalationService;
import org.wf.dp.dniprorada.util.GeneralConfig;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Controller
@Api(tags = { "Электронная эскалация" }, description = "ActivitiRestEscalationController")
@RequestMapping(value = "/escalation")
public class ActivitiRestEscalationController {

    private static final Logger LOG = Logger.getLogger(ActivitiRestEscalationController.class);
    private static final String ERROR_CODE = "exception in escalation-controller!";


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Подробные описания сервисов для документирования в Swagger
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String noteCODE= "\n```\n";    
    private static final String noteController = "#####  Электронная эскалация. ";    

    private static final String noteRunEscalationRule = noteController + "Запуск правила эскалации по его Ид #####\n\n"
            + "правило эскалации -- это запись с указанием БП и задачи, по которым следует отправлять уведомления\n"
            + "в случае \"зависания\", т.е. необработки задач чиновниками.\n\n"
            + "- @param nID - ид правила эскалации\n";

    private static final String noteRunEscalationAll = noteController + "Запуск всех правил эскалаций #####\n\n"
            + "правило эскалации -- это запись с указанием БП и задачи, по которым следует отправлять уведомления\n"
            + "в случае \"зависания\", т.е. необработки задач чиновниками.\n\n";

    private static final String noteSetEscalationRuleFunction = noteController + "Добавление/обновление записи функции эскалации #####\n\n"
            + "HTTP Context: test.region.igov.org.ua/wf/service/escalation/setEscalationRuleFunction\n\n"
            + "параметры:\n\n"
            + "- nID - ИД-номер (уникальный-автоитерируемый), опционально\n"
            + "- sName - строка-название (Например \"Отсылка уведомления на электронную почту\"), обязательно\n"
            + "- sBeanHandler - строка бина-обработчика, опционально\n"
            + "ответ: созданная/обновленная запись.\n\n"
            + "- если nID не задан, то это создание записи\n"
            + "- если nID задан, но его нету -- будет ошибка \"403. Record not found\"\n"
            + "- если nID задан, и он есть -- запись обновляется\n";

    private static final String noteGetEscalationRuleFunction = noteController + "Возврат одной записи функции эскалации по ее nID #####\n\n"
	    + "Если записи нету -- \"403. Record not found\"";

    private static final String noteGetEscalationRuleFunctions = noteController + "Выборка всех записей функции эскалации #####\n\n";

    private static final String noteRemoveEscalationRuleFunction = noteController + "Удаление записи функции эскалации по ее nID #####\n\n"
	        + "Если записи нету -- \"403. Record not found\"";

    private static final String noteSetEscalationRule = noteController + "Добавление/обновление записи правила эскалации #####\n\n"
            + "HTTP Context: test.region.igov.org.ua/wf/service/escalation/setEscalationRule\n\n"
            + "параметры:\n\n"
            + "- nID - ИД-номер (уникальный-автоитерируемый)\n"
            + "- sID_BP - ИД-строка бизнес-процесса\n"
            + "- sID_UserTask - ИД-строка юзертаски бизнеспроцесса (если указана * -- то выбираются все задачи из бизнес-процесса)\n"
            + "- sCondition - строка-условие (на языке javascript )\n"
            + "- soData - строка-обьект, с данными (JSON-обьект)\n"
            + "- sPatternFile - строка файла-шаблона (примеры тут)\n"
            + "- nID_EscalationRuleFunction - ИД-номер функции эскалации\n"
            + "ответ: созданная/обновленная запись.\n\n"
            + "- если nID не задан, то это создание записи\n"
            + "- если nID задан, но его нету -- будет ошибка \"403. Record not found\"\n"
            + "- если nID задан, и он есть -- запись обновляется\n"
            + "ПРИМЕР:\n"
            + "https://test.region.igov.org.ua/wf/service/escalation/setEscalationRule?sID_BP=zaporoshye_mvk-1a&sID_UserTask=*&sCondition=nElapsedDays==nDaysLimit&soData={nDaysLimit:3,asRecipientMail:'test@email.com'}&sPatternFile=escalation/escalation_template.html&nID_EscalationRuleFunction=1\n\n"
            + "ОТВЕТ:\n"
            + noteCODE
            + "  {\n"
            + "    \"sID_BP\":\"zaporoshye_mvk-1a\",\n"
            + "    \"sID_UserTask\":\"*\",\n"
            + "    \"sCondition\":\"nElapsedDays==nDaysLimit\",\n"
            + "    \"soData\":\"{nDaysLimit:3,asRecipientMail:[test@email.com]}\",\n"
            + "    \"sPatternFile\":\"escalation/escalation_template.html\",\n"
            + "    \"nID\":1008,\n"
            + "    \"nID_EscalationRuleFunction\":\n"
            + "    {\"sBeanHandler\":\"EscalationHandler_SendMailAlert\",\n"
            + "      \"nID\":1,\n"
            + "      \"sName\":\"Send Email\"\n"
            + "    }\n"
            + "  }\n"
            + noteCODE;

    private static final String noteGetEscalationRule = noteController + "Возврат одной записи правила эскалации по ее nID #####\n\n"
	    + "если записи нету -- \"403. Record not found\"";

    private static final String noteGetEscalationRules = noteController + "Возвращает список всех записей правил ескалации #####\n\n";

    private static final String noteRemoveEscalationRule = noteController + "Удаление записи правила эскалации по ее nID #####\n\n"
	    + "если записи нету -- \"403. Record not found\"";

    private static final String noteGetEscalationHistory = noteController + "Возвращает массив объектов сущности по заданним параметрам #####\n\n"
	        + "Возвращает не больше 5000 записей\n"
	        + "Пример 1: https://test.igov.org.ua/wf/service/escalation/getEscalationHistory\n\n"
	        + "Пример ответа:\n\n"
	        + noteCODE
	        + "  [{\n"
	        + "    \"sDate\":\"2015-09-09 21:20:25.000\",\n"
	        + "    \"nID\":1,\n"
	        + "    \"nID_Process\":9463,\n"
	        + "    \"nID_Process_Root\":29193,\n"
	        + "    \"nID_UserTask\":894,\n"
	        + "    \"nID_EscalationStatus\":91\n"
	        + "  }\n"
	        + "  ...\n"
	        + "  ]\n"
	        + noteCODE
	        + "Пример 2:\n https://test.igov.org.ua/wf/service/escalation/getEscalationHistory?nID_Process=6276&nID_Process_Root=57119&nID_UserTask=634&sDateStart=2014-11-24%2000:03:00&sDateEnd=2014-12-26%2000:03:00&nRowsMax=100\n\n"
	        + "Пример ответа: записи, попадающие под критерии параметров в запросе\n\n"
	        + "- nIdProcess     номер-ИД процесса //опциональный\n"
	        + "- nIdProcessRoot номер-ИД процесса (корневого) //опциональный\n"
	        + "- nIdUserTask    номер-ИД юзертаски //опциональный\n"
	        + "- sDateStart     дата начала выборки //опциональный, в формате YYYY-MM-DD hh:mm:ss\n"
	        + "- sDateEnd       дата конца выборки //опциональный, в формате YYYY-MM-DD hh:mm:ss\n"
	        + "- nRowsMax       максимальное число строк //опциональный, по умолчанию 100 (защита - не более 5000)\n";

    private static final String noteGetEscalationStatuses = noteController + "Возвращает массив объектов сущности EscalationStatus #####\n\n"
            + "Возвращает массив объектов сущности EscalationStatus\n"
            + "Пример: https://<server>/wf/service/escalation/getEscalationStatuses\n\n"
            + "Пример ответа:\n\n"
            + noteCODE
            + "[\n"
            + "{\"sNote\":\"Отослано письмо\",\"nID\":1,\"sID\":\"MailSent\"},\n"
            + "{\"sNote\":\"БП создан\",\"nID\":2,\"sID\":\"BP_Created\"},\n"
            + "{\"sNote\":\"БП в процессе\",\"nID\":3,\"sID\":\"BP_Process\"},\n"
            + "{\"sNote\":\"БП закрыт\",\"nID\":4,\"sID\":\"BP_Closed\"}\n"
            + "]\n"
            + noteCODE;
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    
    @Autowired
    GeneralConfig generalConfig;
    @Autowired
    private EscalationRuleFunctionDao escalationRuleFunctionDao;
    @Autowired
    private EscalationRuleDao escalationRuleDao;
    @Autowired
    private EscalationService escalationService;
    @Autowired
    private EscalationHistoryDao escalationHistoryDao;
    @Autowired
    private EscalationStatusDao escalationStatusDao;

    /**
     * запуск правила эскалации по его Ид
     * правило эскалации -- это запись с указанием бп и задачи, по которым следует отправлять уведомления
     * в случае "зависания", т.е. необработки задач чиновниками.
     *
     * @param nID - ид правила эскалации
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Запуск правила эскалации по его Ид ", notes = noteRunEscalationRule )
    @RequestMapping(value = "/runEscalationRule", method = RequestMethod.GET)
    @ResponseBody
    public void runEscalationRule( @ApiParam(value = "ид правила эскалации", required = true) @RequestParam(value = "nID") Long nID) throws ActivitiRestException {
        escalationService.runEscalationRule(nID, generalConfig.sHost());
    }

    /**
     * запуск всех правил эскалаций
     * правило эскалации -- это запись с указанием бп и задачи, по которым следует отправлять уведомления
     * в случае "зависания", т.е. необработки задач чиновниками.
     *
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Запуск всех правил эскалаций ", notes = noteRunEscalationAll )
    @RequestMapping(value = "/runEscalation", method = RequestMethod.GET)
    @ResponseBody
    public void runEscalationAll() throws ActivitiRestException {
        escalationService.runEscalationAll();
    }

    //----------EscalationRuleFunction services-----------------

    /**
     * добавление/обновление записи функции эскалации
     * если nID не задан, то это создание записи
     * если nID задан, но его нету -- будет ошибка "403. Record not found"
     * если nID задан, и он есть -- запись обновляется
     *
     * @param nID          -- ИД-номер (уникальный-автоитерируемый), опционально
     * @param sName        -- строка-название (Например "Отсылка уведомления на электронную почту"), обязательно
     * @param sBeanHandler -- строка бина-обработчика, опционально
     * @return созданная/обновленная запись.
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Добавление/обновление записи функции эскалации", notes = noteSetEscalationRuleFunction )
    @RequestMapping(value = "/setEscalationRuleFunction", method = RequestMethod.GET)
    @ResponseBody
    public EscalationRuleFunction setEscalationRuleFunction(
	    @ApiParam(value = "ИД-номер (уникальный-автоитерируемый)", required = false) @RequestParam(value = "nID", required = false) Long nID,
	    @ApiParam(value = "строка-название (Например \"Отсылка уведомления на электронную почту\")", required = true) @RequestParam(value = "sName") String sName,
	    @ApiParam(value = "строка бина-обработчика", required = false) @RequestParam(value = "sBeanHandler", required = false) String sBeanHandler)
            throws ActivitiRestException {

        try {
            return escalationRuleFunctionDao.saveOrUpdate(nID, sName, sBeanHandler);
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }

    }

    /**
     * возврат одной записи функции эскалации по ее nID, если записи нету -- "403. Record not found"
     *
     * @param nID -- nID функции эскалации
     * @return запись функции эскалации по ее nID, если записи нету -- "403. Record not found"
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Возврат одной записи функции эскалации по ее nID ", notes = noteGetEscalationRuleFunction )
    @ApiResponses(value = { @ApiResponse(code = 403, message = "Record not found") })
    @RequestMapping(value = "/getEscalationRuleFunction", method = RequestMethod.GET)
    @ResponseBody
    public EscalationRuleFunction getEscalationRuleFunction(
	    @ApiParam(value = "nID функции эскалации", required = true) @RequestParam(value = "nID") Long nID) throws ActivitiRestException {

        EscalationRuleFunction ruleFunction = escalationRuleFunctionDao.findById(nID).orNull();
        if (ruleFunction == null) {
            throw new ActivitiRestException(
                    ActivitiExceptionController.BUSINESS_ERROR_CODE,
                    "Record not found. No such EscalationRuleFunction with nID=" + nID,
                    HttpStatus.FORBIDDEN);
        }
        return ruleFunction;
    }

    /**
     * выборка всех записей функции эскалации
     *
     * @return все записи функций эскалации
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Выборка всех записей функции эскалации", notes = noteGetEscalationRuleFunctions )
    @RequestMapping(value = "/getEscalationRuleFunctions", method = RequestMethod.GET)
    @ResponseBody
    public List<EscalationRuleFunction> getEscalationRuleFunctions()
            throws ActivitiRestException {

        try {
            return escalationRuleFunctionDao.findAll();
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }
    }

    /**
     * удаление записи функции эскалации по ее nID, если записи нету -- "403&#183; Record not found"
     *
     * @param nID -- nID функции эскалации
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Удаление записи функции эскалации по ее nID", notes = noteRemoveEscalationRuleFunction )
    @ApiResponses(value = { @ApiResponse(code = 403, message = "Record not found") })
    @RequestMapping(value = "/removeEscalationRuleFunction", method = RequestMethod.GET)
    @ResponseBody
    public void removeEscalationRuleFunction(
	    @ApiParam(value = "nID функции эскалации", required = true) @RequestParam(value = "nID") Long nID) throws ActivitiRestException {

        try {
            escalationRuleFunctionDao.delete(nID);
        } catch (EntityNotFoundException e) {
            throw new ActivitiRestException(
                    ActivitiExceptionController.BUSINESS_ERROR_CODE,
                    e.getMessage(),
                    e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }
    }

    //----------EscalationRule services-----------------

    /**
     * добавление/обновление записи правила эскалации
     * если nID не задан, то это создание записи
     * если nID задан, но его нету -- будет ошибка "403\. Record not found"
     * если nID задан, и он есть -- запись обновляется
     * ПРИМЕР: test.region.igov.org.ua/wf/service/escalation/setEscalationRule
     * ?sID_BP=zaporoshye_mvk-1a&sID_UserTask=*&sCondition=nElapsedDays==nDaysLimit
     * &soData={nDaysLimit:3,asRecipientMail:['test@email.com']}
     * &sPatternFile=escalation/escalation_template.html&nID_EscalationRuleFunction=1
     *
     * @param nID                        - ИД-номер (уникальный-автоитерируемый)
     * @param sID_BP                     - ИД-строка бизнес-процесса
     * @param sID_UserTask               - ИД-строка юзертаски бизнеспроцесса (если указана * -- то выбираются все задачи из бизнес-процесса)
     * @param sCondition                 - строка-условие (на языке javascript )
     * @param soData                     - строка-обьект, с данными (JSON-обьект)
     * @param sPatternFile               - строка файла-шаблона (примеры тут)
     * @param nID_EscalationRuleFunction - ИД-номер функции эскалации
     * @return созданная/обновленная запись.
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Добавление/обновление записи правила эскалации", notes = noteSetEscalationRule )
    @ApiResponses(value = { @ApiResponse(code = 403, message = "Record not found") })
    @RequestMapping(value = "/setEscalationRule", method = RequestMethod.GET)
    @ResponseBody
    public EscalationRule setEscalationRule(
	    @ApiParam(value = "ИД-номер (уникальный-автоитерируемый)", required = false)  @RequestParam(value = "nID", required = false) Long nID,
	    @ApiParam(value = "ИД-строка бизнес-процесса", required = true) @RequestParam(value = "sID_BP") String sID_BP,
	    @ApiParam(value = "ИД-строка юзертаски бизнеспроцесса (если указана * -- то выбираются все задачи из бизнес-процесса)", required = true) @RequestParam(value = "sID_UserTask") String sID_UserTask,
	    @ApiParam(value = "строка-условие (на языке javascript )", required = true) @RequestParam(value = "sCondition") String sCondition,
	    @ApiParam(value = "строка-обьект, с данными (JSON-обьект)", required = true) @RequestParam(value = "soData") String soData,
	    @ApiParam(value = "строка файла-шаблона (примеры тут)", required = true) @RequestParam(value = "sPatternFile") String sPatternFile,
	    @ApiParam(value = "ИД-номер функции эскалации", required = true) @RequestParam(value = "nID_EscalationRuleFunction") Long nID_EscalationRuleFunction)
            throws ActivitiRestException {

        try {
            EscalationRuleFunction ruleFunction = null;
            if (nID_EscalationRuleFunction != null) {
                ruleFunction = escalationRuleFunctionDao.findById(nID_EscalationRuleFunction).orNull();
            }
            return escalationRuleDao.saveOrUpdate(nID, sID_BP, sID_UserTask,
                    sCondition, soData, sPatternFile, ruleFunction);
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }

    }

    /**
     * возврат одной записи правила эскалации по ее nID, если записи нету -- "403. Record not found"
     *
     * @param nID - nID правила эскалации
     * @return правило эскалации по ее nID, если записи нету -- "403. Record not found"
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Возврат одной записи правила эскалации по ее nID", notes = noteGetEscalationRule )
    @ApiResponses(value = { @ApiResponse(code = 403, message = "Record not found") })
    @RequestMapping(value = "/getEscalationRule", method = RequestMethod.GET)
    @ResponseBody
    public EscalationRule getEscalationRule(
	    @ApiParam(value = "nID правила эскалации", required = true) @RequestParam(value = "nID") Long nID) throws ActivitiRestException {

        EscalationRule rule = escalationRuleDao.findById(nID).orNull();
        if (rule == null) {
            throw new ActivitiRestException(
                    ActivitiExceptionController.BUSINESS_ERROR_CODE,
                    "Record not found. No such EscalationRule with nID=" + nID,
                    HttpStatus.FORBIDDEN);
        }
        return rule;
    }

    /**
     * возвращает список всех записей правил ескалации
     *
     * @return список всех записей правил ескалации
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Возвращает список всех записей правил ескалации", notes = noteGetEscalationRules )
    @RequestMapping(value = "/getEscalationRules", method = RequestMethod.GET)
    @ResponseBody
    public List<EscalationRule> getEscalationRules() throws ActivitiRestException {
        try {
            return escalationRuleDao.findAll();
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }
    }

    /**
     * удаление записи правила эскалации по ее nID, если записи нету -- "403. Record not found"
     *
     * @param nID - nID правила эскалации
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Удаление записи правила эскалации по ее nID", notes = noteRemoveEscalationRule )
    @ApiResponses(value = { @ApiResponse(code = 403, message = "Record not found") })
    @RequestMapping(value = "/removeEscalationRule", method = RequestMethod.GET)
    @ResponseBody
    public void removeEscalationRule(
	    @ApiParam(value = "nID правила эскалации", required = true) @RequestParam(value = "nID") Long nID) throws ActivitiRestException {

        try {
            escalationRuleDao.delete(nID);
        } catch (EntityNotFoundException e) {
            throw new ActivitiRestException(
                    ActivitiExceptionController.BUSINESS_ERROR_CODE,
                    e.getMessage(),
                    e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }
    }

    //------------------------------Escalation History services----------------------------------
    /**
     * Возвращает массив объектов сущности по заданним параметрам (но не больше 5000 записей)
     * Пример 1: https://test.igov.org.ua/wf/service/escalation/getEscalationHistory
     *
     * Пример ответа:
     * [{
     *      "sDate":"2015-09-09 21:20:25.000",
     *      "nID":1,
     *      "nID_Process":9463,
     *      "nID_Process_Root":29193,
     *      "nID_UserTask":894,
     *      "nID_EscalationStatus":91
     *  }
     *  ...
     * ]
     *
     * Пример 2: https://test.igov.org.ua/wf/service/escalation/getEscalationHistory?nID_Process=6276&nID_Process_Root=57119&nID_UserTask=634&sDateStart=2014-11-24%2000:03:00&sDateEnd=2014-12-26%2000:03:00&nRowsMax=100
     *
     * Пример ответа: записи, попадающие под критерии параметров в запросе
     *
     * @param nIdProcess     номер-ИД процесса //опциональный
     * @param nIdProcessRoot номер-ИД процесса (корневого) //опциональный
     * @param nIdUserTask    номер-ИД юзертаски //опциональный
     * @param sDateStart     дата начала выборки //опциональный, в формате YYYY-MM-DD hh:mm:ss
     * @param sDateEnd       дата конца выборки //опциональный, в формате YYYY-MM-DD hh:mm:ss
     * @param nRowsMax       максимальное число строк //опциональный, по умолчанию 100 (защита - не более 5000)
     * @return List<EscalationHistory>
     * @throws ActivitiRestException
     */
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "Возвращает массив объектов сущности по заданним параметрам", notes = noteGetEscalationHistory )
    @RequestMapping(value = "/getEscalationHistory", method = RequestMethod.GET)
    @ResponseBody
    public List<EscalationHistory> getEscalationHistory(
	    @ApiParam(value = "номер-ИД процесса", required = false) @RequestParam(value = "nID_Process", required = false) Long nIdProcess,
	    @ApiParam(value = "номер-ИД процесса (корневого)", required = false) @RequestParam(value = "nID_Process_Root", required = false) Long nIdProcessRoot,
	    @ApiParam(value = "номер-ИД юзертаски", required = false) @RequestParam(value = "nID_UserTask", required = false) Long nIdUserTask,
	    @ApiParam(value = "дата начала выборки", required = false) @RequestParam(value = "sDateStart", required = false) String sDateStart,
	    @ApiParam(value = "дата конца выборки", required = false) @RequestParam(value = "sDateEnd", required = false) String sDateEnd,
	    @ApiParam(value = "максимальное число строк, по умолчанию 100 (защита - не более 5000)", required = false) @RequestParam(value = "nRowsMax", required = false) Integer nRowsMax) throws ActivitiRestException {
        try {
            DateTime startDate = null;
            DateTime endDate = null;
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            if (sDateStart != null) {
                startDate = formatter.parseDateTime(sDateStart);
            }
            if (sDateEnd != null) {
                endDate = formatter.parseDateTime(sDateEnd);
            }

            return escalationHistoryDao.getAllByCriteria(nIdProcess, nIdProcessRoot, nIdUserTask, startDate, endDate, nRowsMax);
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }
    }

    //------------------------------------Escalation Status services--------------------------------

    /**
     * Возвращает массив объектов сущности EscalationStatus
     * Пример: https://<server>/wf/service/escalation/getEscalationStatuses
     *
     * Пример ответа:
     * [
     *  {"sNote":"Отослано письмо","nID":1,"sID":"MailSent"},
     *  {"sNote":"БП создан","nID":2,"sID":"BP_Created"},
     *  {"sNote":"БП в процессе","nID":3,"sID":"BP_Process"},
     *  {"sNote":"БП закрыт","nID":4,"sID":"BP_Closed"}
     * ]
     *
     * @return List<EscalationStatus>
     * @throws ActivitiRestException
     */
    @ApiOperation(value = "Возвращает массив объектов сущности EscalationStatus", notes = noteGetEscalationStatuses )
    @RequestMapping(value = "/getEscalationStatuses", method = RequestMethod.GET)
    @ResponseBody
    public List<EscalationStatus> getEscalationStatuses() throws ActivitiRestException {
        try {
            return escalationStatusDao.findAll();
        } catch (Exception e) {
            throw new ActivitiRestException(ERROR_CODE, e);
        }


    }
}
