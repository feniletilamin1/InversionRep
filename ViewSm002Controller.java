package ru.inversion.fxsmev.sm002;

import javafx.fxml.FXML;
import javafx.scene.control.Separator;
import ru.inversion.bicomp.action.JInvButtonPrint;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fxsmev.cmn.common.WspProperties;
import ru.inversion.fxsmev.cmn.sm002.PSm002;
import ru.inversion.fxsmev.cmn.sm002.PSm002Dsc;
import ru.inversion.fxsmev.cmn.sm002.Sm002SettingsEnum;
import ru.inversion.fxsmev.common.AbstractViewSmController;
import ru.inversion.fxsmev.common.BaseSecOptions;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.control.ButtonBase;
import javax.naming.ConfigurationException;
import org.slf4j.Logger;

import static ru.inversion.dataset.DataLinkBuilder.linkDataSet;
import ru.inversion.dataset.SQLDataSet;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_DEL;
import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.*;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fxsmev.cmn.common.DtoResponse;
import static ru.inversion.fxsmev.cmn.common.DtoResponse.TOO_MANY_REPEAT;
import ru.inversion.fxsmev.common.PCusData;
import ru.inversion.fxsmev.common.SMDao;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.tc.TaskContext;

/**
 *
 * @author ssu
 * @since Thu Nov 30 19:58:43 MSK 2017
 */
public class ViewSm002Controller extends AbstractViewSmController< PSm002, Sm002SettingsEnum> {

    private static final Long REPORT_TYPE_ID = 4L;
    @FXML
    private JInvTable< PSm002> V_SM_002;
    @FXML
    private JInvTable< PSm002Dsc> V_SM_002_DSC;
    @FXML
    private JInvTextArea CRESULT_INFO;
    @FXML
    private JInvTextField CRES_CODE_NAME;
    @FXML
    private JInvTextArea CNAME_PRSN_EX;

    private final XXIDataSet< PSm002> dsV_SM_002 = new XXIDataSet<>();
    private final XXIDataSet< PSm002Dsc> dsV_SM_002Dsc = new XXIDataSet<>();

    /**
     * Набор прав безопасности
     */
    public static class Sm002SecOptions extends BaseSecOptions {

        public Sm002SecOptions() {
            super();
        }

        @Override
        public int[] getActionList() {
            return new int[]{3904, 3905, 3906, 3907, 3908, 3909, 3914};
        }

        @Override
        public String[] getRoleList() {
            return new String[]{};
        }
    }

    private JInvButtonPrint btPrint;

    @Override
    protected Class<Sm002SecOptions> getSecOptionClass() {
        return Sm002SecOptions.class;
    }

    @Override
    protected Integer getSSCODE() {
        return 356;
    }

    /**
     *
     */
    private void initDataSet() throws Exception {

        dsV_SM_002.setTaskContext(getTaskContext());
        dsV_SM_002.setRowClass(PSm002.class);

        dsV_SM_002Dsc.setTaskContext(getTaskContext());
        dsV_SM_002Dsc.setRowClass(PSm002Dsc.class);
    }

    @Override
    protected URL getPlSQLUrlForUpdateState() {
        return PSm002.class.getResource("plsql/def.xml");
    }

    /**
     *
     */
    @Override
    protected void init() throws Exception {

        super.init();

        setTitle(getBundleString("VIEW.TITLE"));

        initDataSet();
        DSFXAdapter< PSm002> dsfx = DSFXAdapter.bind(dsV_SM_002, V_SM_002, null, true);
        dsfx.setEnableFilter(true);
        dsfx.bindControl(CRESULT_INFO, CRES_CODE_NAME, CNAME_PRSN_EX);

        DSFXAdapter< PSm002Dsc> dsfxDsc = DSFXAdapter.bind(dsV_SM_002Dsc, V_SM_002_DSC, null, false);
        linkDataSet(dsV_SM_002, dsV_SM_002Dsc, PSm002::getIDSM_ENTRY, "IDSM_ENTRY");

        V_SM_002.setToolBar(toolBar);
        V_SM_002.setAction(CREATE, a -> checkFor(CREATE, FormModeEnum.VM_INS));
        V_SM_002.setAction(VIEW, a -> doOperation(FormModeEnum.VM_SHOW));
        V_SM_002.setAction(UPDATE, a -> checkFor(UPDATE, FormModeEnum.VM_EDIT));
        V_SM_002.setAction(DELETE, a -> checkFor(DELETE, FormModeEnum.VM_DEL));
        V_SM_002.setAction(REFRESH, a -> doRefresh());

        doRefresh();

    }

    /**
     *
     */
    @Override
    protected int getWspId() {
        return 0x2;
    }

    /**
     *
     */
    @Override
    protected WspProperties<Sm002SettingsEnum> getWspProperties() {
        return Sm002SettingsEnum::values;
    }

    /**
     *
     */
    @Override
    protected PSm002 getCurrentRow() {
        return dsV_SM_002.getCurrentRow();
    }

    @Override
    protected void refreshCurrentEntry() throws Exception {
        dsV_SM_002.refreshCurrentRowFromDB(true);
    }

    @Override
    protected JInvTable< PSm002> getMainTable() {
        return V_SM_002;
    }

    //
    private void doRefresh() {
        V_SM_002.executeQuery();
    }

    /**
     *
     */
    @Override
    protected void initToolBar() {
        super.initToolBar();

        btPrint = new JInvButtonPrint(rep -> {
            rep.setTypeID(REPORT_TYPE_ID);

            PSm002 currentRow = dsV_SM_002.getCurrentRow();
            Long markerID = dsV_SM_002.getMarkerID();
            rep.setParam2(currentRow.getIDSM_ENTRY(), markerID);
        });

        final ButtonBase btTest = ActionFactory.createButton(
                new ActionBuilder()
                        .handler((t) -> {
                            Sm002App.runService(viewContext, taskContext, "7825706086");
                        })
                        .icon(
                                new IconDescriptorBuilder()
                                        .iconId(FontAwesome.fa_indent)
                                        .build())
                        .build()
        );

        toolBar.getItems().addAll(
                btTest,
                new Separator(),
                btPrint
        );

    }

    /**
     *
     */
    protected Integer getSecurityId(Object actionId) {

        if (actionId != null) {
            if (actionId instanceof ActionFactory.ActionTypeEnum) {

                return U.<ActionFactory.ActionTypeEnum, Integer>decode(
                        (ActionFactory.ActionTypeEnum) actionId,
                        CREATE, 3905,
                        UPDATE, 3906,
                        DELETE, 3907,
                        RUN, 3908,
                        CONFIG, 3909
                );
            }

            if (S.isString(actionId)) {

                if ("ENTER_WSP".equals((String) actionId)) {
                    return 3904;
                }
            }
        }
        return super.getSecurityId(actionId);
    }

    /**
     *
     */
    private boolean checkFor(Object actionId, JInvFXFormController.FormModeEnum mode) {

        try {

            if (checkSecurityFor(actionId)) {
                doOperation(mode);
                return true;
            }
        } catch (Throwable th) {
            handleException(th);
        }

        return false;
    }

    //
    private void doOperation(JInvFXFormController.FormModeEnum mode) {

        if (mode == VM_DEL && handleDeleteByMark(dsV_SM_002)) {
            return;
        }

        PSm002 p = null;

        switch (mode) {
            case VM_INS:
                p = new PSm002();
                break;
            case VM_EDIT:
            case VM_SHOW:
            case VM_DEL:
                p = dsV_SM_002.getCurrentRow();
                break;
        }

        if (p != null) {
            new FXFormLauncher< PSm002>(
                    getTaskContext(),
                    getViewContext(),
                    EditSm002Controller.class,
                    ResourceBundle.getBundle("ru.inversion.fxsmev.cmn.sm002.res.Sm002")
            )
                    .dataObject(p)
                    .dialogMode(mode)
                    .initProperties(getInitProperties())
                    .callback(this::doFormResult)
                    .modal(true)
                    .show();
        }
    }

//
//
//
    private void doFormResult(JInvFXFormController.FormReturnEnum ok, JInvFXFormController< PSm002> dctl) {
        try {

            if (FormReturnEnum.RET_OK == ok) {
                switch (dctl.getFormMode()) {
                    case VM_INS:
                        dsV_SM_002.insertRow(dctl.getDataObject(), IDataSet.InsertRowModeEnum.AFTER_CURRENT, true);
                        dsV_SM_002.refreshCurrentRowFromDB();
                        break;
                    case VM_EDIT:
                        dsV_SM_002.updateCurrentRow(dctl.getDataObject());
                        break;
                    case VM_DEL:
                        dsV_SM_002.removeCurrentRow();
                        break;
                    default:
                        break;
                }
            }

            V_SM_002.requestFocus();

        } catch (Throwable th) {
            handleException(th);
        }
    }

    public static int sendSm002Service(ViewContext vc, TaskContext tc, Long IDSM_ENTRY, Logger appLoger) throws ConfigurationException {

        DtoResponse dtoResponseSm002 = getSmevClientStatic(SMDao.getPreferences(tc)).syncRequest("<request wsp=\"sm002\" action=\"send\"><data><value>" + IDSM_ENTRY
                + "</value></data></request>");

        tc.commit();

        if (dtoResponseSm002.code() == TOO_MANY_REPEAT.code()) {
            appLoger.info("Ответ не был получен, код ответа 2001");
            return 2; // Если не смогли получить ответ сразу и отлетаем по таймауту, то возвращаем код 2
        }

        if (dtoResponseSm002.isSuccess()) {
            appLoger.info("Ответ был получен, ищем приостановления по счетам");
            int stopsCount = checkClientStops(tc, IDSM_ENTRY);
            appLoger.info("Количество приостановлений по счетам: " + stopsCount);

            if (stopsCount != 0) {
                return 1;
            } else {
                return 0;
            }
//   
        } else if (dtoResponseSm002.isFault()) {
            return -1;
        }
        return 0;
    }

    //  Получить Id клиента по инн  
    public static Optional<PCusData> getCusIdByInn(TaskContext taskContext, String inn) {
        try {

            return Optional.ofNullable(
                    new SQLDataSet<>(taskContext, PCusData.class
                    )
                            .singleRow()
                            .wherePredicat("CCUSNUMNAL= '" + inn + "'")
                            .execute()
                            .getCurrentRow()
            );
        } catch (Throwable th) {
            throw new RuntimeException("Error on execute SQL in getCusIdByInn() for getting cusId", th);
        }
    }

    //  Получить Id клиента по инн  
    public static Optional<PCusData> getCusIdByKio(TaskContext taskContext, String KIO) {
        try {

            return Optional.ofNullable(
                    new SQLDataSet<>(taskContext, PCusData.class
                    )
                            .singleRow()
                            .wherePredicat("CCUSIIN= '" + KIO + "'")
                            .execute()
                            .getCurrentRow()
            );
        } catch (Throwable th) {
            throw new RuntimeException("Error on execute SQL in getCusIdByInn() for getting cusId", th);
        }
    }

    public static int checkClientStops(TaskContext tc, Long IDSM_ENTRY) {
        try {
            return new SQLDataSet<>(tc, PSm002Dsc.class
            )
                    .wherePredicat("IDSM_ENTRY= '" + IDSM_ENTRY + "'")
                    .execute()
                    .getLoadedRowCount();
        } catch (Throwable th) {
            throw new RuntimeException("Error on execute SQL in checkClientStops() for getting client count stops on acc", th);
        }
    }
}
