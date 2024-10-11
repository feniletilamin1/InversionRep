package ru.inversion.fxsmev.sm002;

import ru.inversion.annotation.StartMethod;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import ru.inversion.bicomp.util.ParamMap;
import ru.inversion.fxsmev.cmn.sm002.PSm002;
import ru.inversion.fxsmev.cmn.sm002.ReqTypeEnum;
import ru.inversion.fxsmev.common.PCusData;
import static ru.inversion.fxsmev.sm002.ViewSm002Controller.getCusIdByInn;
import static ru.inversion.fxsmev.sm002.ViewSm002Controller.getCusIdByKio;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 * СМЭВ.3
 *
 * Стартовый класс: Запросы о наличии решений приостановлений операций по счетам
 *
 * @author Sulimoff
 */
public class Sm002App extends BaseApp {

    @Override
    protected void showMainWindow() {
        showViewSm002(getPrimaryViewContext(), null/*new TaskContext ()*/, Collections.emptyMap());
    }

    @Override
    public String getAppID() {
        return "XXI.SM002";
    }

    public static void main(String[] args) {
        launch(args);
    }

    @StartMethod(description = "СМЭВ.3 (002) Сведения о наличии решений о приостановлении операций по счетам")
    public static void showViewSm002(ViewContext vc, TaskContext tc, Map<String, Object> p) {
        new FXFormLauncher<>(tc, vc, ViewSm002Controller.class,
                ResourceBundle.getBundle("ru.inversion.fxsmev.cmn.sm002.res.Sm002"))
                .initProperties(p)
                .show();
    }

    /**
     * Точка входа внешнего вызова отправки запроса по ИНН/КИО клиента в СМЭВ.3
     * (002) Сведения о наличии решений о приостановлении операций по счетам
     *
     * Коды возврата: // 1 - Есть приостановления // 0 - Нет приостановлений // 2 - Не смогли получить ответ по тайм-ауту // -1 - Ошибка
     */
    public static int runService(ViewContext vc, TaskContext tc, String inn) {
        appLog.info("Вызов внешней функции получения приостановлений по клиенту");
        appLog.debug("ИНН клиента:" + inn);
        try {
            if (S.isNullOrEmpty(inn)) {
                throw new IllegalArgumentException("Отсутствует инн");
            }

            Optional<PCusData> cus;

            ReqTypeEnum dataType = U.decode(inn.length(), // Определяем тип данных ИНН ФИЗ/ЮР. лица или КИО
                    5, ReqTypeEnum.KIO,
                    10, ReqTypeEnum.INN_JL,
                    12, ReqTypeEnum.INN_FL,
                    ReqTypeEnum.INN_FL);

            appLog.debug("Тип полученных данных: " + dataType);

            //          Получаем клиента 
            if (dataType == ReqTypeEnum.KIO) {
                cus = getCusIdByKio(tc, inn);
            } else {
                cus = getCusIdByInn(tc, inn);
            }

//          Если клиент с таким инн/кио не найдем, то выкидываем ошибку  
            if (!cus.isPresent()) {
                throw new IllegalArgumentException("Клиент с инн/кио " + inn + " не найден в каталоге");
            }
//          Собираем ParamMap для того чтобы создать запрос на получение приостановлений о счетах по клиенту в базе  
            ParamMap insertMap = new ParamMap();

            appLog.info("Данные запроса");
            appLog.debug("IDSM_PRVD:" + 2);
            appLog.debug("IREQ_TYPE:" + dataType.toInt());
            appLog.debug("CREQ_DATA:" + inn);
            appLog.debug("CNAME_PRSN:" + cus.get().getCCUSNAME());
            appLog.debug("ICUSNUM:" + cus.get().getICUSNUM());

            insertMap.put("IDSM_PRVD", 2); // Ид вида сведений
            insertMap.put("IREQ_TYPE", dataType.toInt()); // тип данных ИНН ФИЗ/ЮР. лица или КИО
            insertMap.put("CREQ_DATA", inn); // инн
            insertMap.put("CNAME_PRSN", cus.get().getCCUSNAME()); // Наименование клиента 
            insertMap.put("ICUSNUM", cus.get().getICUSNUM()); // Ид клиента

            insertMap.exec(tc, PSm002.class.getResource("plsql/def.xml"), "PSm002.insert"); // Добавляем новый запрос в базу
            tc.commit();

            Long request_IDSM_ENTRY = insertMap.getLong("IDSM_ENTRY"); // IDSM_ENTRY нового запроса

            appLog.debug("Создан запрос с IDSM_ENTRY " + request_IDSM_ENTRY);

            int resultCode = ViewSm002Controller.sendSm002Service(vc, tc, request_IDSM_ENTRY, appLog); // Отправляем запрос

            appLog.debug("Код обработки функции: " + resultCode);

            return resultCode;
        } catch (Throwable th) {
            appLog.error(th.getMessage(), th);
            tc.rollback();
            JInvErrorService.handleException(vc, th);
        }
        return -1;
    }
}
