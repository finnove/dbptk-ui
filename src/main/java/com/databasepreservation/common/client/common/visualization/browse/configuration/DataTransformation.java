package com.databasepreservation.common.client.common.visualization.browse.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.databasepreservation.common.client.ViewerConstants;
import com.databasepreservation.common.client.common.LoadingDiv;
import com.databasepreservation.common.client.common.RightPanel;
import com.databasepreservation.common.client.common.breadcrumb.BreadcrumbItem;
import com.databasepreservation.common.client.common.breadcrumb.BreadcrumbPanel;
import com.databasepreservation.common.client.common.lists.widgets.MultipleSelectionTablePanel;
import com.databasepreservation.common.client.common.sidebar.DataTransformationSidebar;
import com.databasepreservation.common.client.common.visualization.browse.configuration.handler.DataTransformationUtils;
import com.databasepreservation.common.client.models.status.collection.CollectionStatus;
import com.databasepreservation.common.client.models.status.denormalization.DenormalizeConfiguration;
import com.databasepreservation.common.client.models.status.denormalization.RelatedTablesConfiguration;
import com.databasepreservation.common.client.models.structure.ViewerColumn;
import com.databasepreservation.common.client.models.structure.ViewerDatabase;
import com.databasepreservation.common.client.models.structure.ViewerForeignKey;
import com.databasepreservation.common.client.models.structure.ViewerJob;
import com.databasepreservation.common.client.models.structure.ViewerJobStatus;
import com.databasepreservation.common.client.models.structure.ViewerReference;
import com.databasepreservation.common.client.models.structure.ViewerTable;
import com.databasepreservation.common.client.services.ConfigurationService;
import com.databasepreservation.common.client.services.JobService;
import com.databasepreservation.common.client.tools.BreadcrumbManager;
import com.databasepreservation.common.client.tools.FontAwesomeIconManager;
import com.databasepreservation.common.client.tools.HistoryManager;
import com.databasepreservation.common.client.widgets.Alert;
import com.databasepreservation.common.client.widgets.BootstrapCard;
import com.databasepreservation.common.client.widgets.SwitchBtn;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Gabriel Barros <gbarros@keep.pt>
 */
public class DataTransformation extends RightPanel {
  interface DataTransformerUiBinder extends UiBinder<Widget, DataTransformation> {
  }

  private static DataTransformerUiBinder binder = GWT.create(DataTransformerUiBinder.class);
  private static Map<String, DataTransformation> instances = new HashMap<>();
  private static Map<String, DenormalizeConfiguration> denormalizeConfigurationList = new HashMap<>();

  @UiField
  public ClientMessages messages = GWT.create(ClientMessages.class);

  @UiField
  FlowPanel content;

  @UiField
  ScrollPanel scrollPanel;

  @UiField
  LoadingDiv loading;

  private ViewerDatabase database;
  private ViewerTable table;
  private TransformationTable rootTable;
  private DataTransformationSidebar sidebar;
  private CollectionStatus collectionStatus;
  private DenormalizeConfiguration denormalizeConfiguration;
  private Button btnSaveConfiguration = new Button();
  private Button btnSaveAllConfiguration = new Button();
  private Button btnRunConfiguration = new Button();
  private Button btnRunAllConfiguration = new Button();
  private Button btnClearConfiguration = new Button();
  private Boolean isInformation;

  @Override
  public void handleBreadcrumb(BreadcrumbPanel breadcrumb) {
    List<BreadcrumbItem> breadcrumbItems = BreadcrumbManager.forDataTransformation(database.getUuid(),
      database.getMetadata().getName());
    BreadcrumbManager.updateBreadcrumb(breadcrumb, breadcrumbItems);
  }

  public static DataTransformation getInstance(CollectionStatus collectionStatus, ViewerDatabase database,
    DataTransformationSidebar sidebar) {
    return instances.computeIfAbsent(database.getUuid(),
      k -> new DataTransformation(collectionStatus, database, null, sidebar));
  }

  public static DataTransformation getInstance(CollectionStatus collectionStatus, ViewerDatabase database,
    String tableUUID, DataTransformationSidebar sidebar) {
    return instances.computeIfAbsent(database.getUuid() + tableUUID,
      k -> new DataTransformation(collectionStatus, database, tableUUID, sidebar));
  }

  private DataTransformation(CollectionStatus collectionStatus, ViewerDatabase database, String tableUUID,
    DataTransformationSidebar sidebar) {
    initWidget(binder.createAndBindUi(this));
    this.database = database;
    this.sidebar = sidebar;
    this.collectionStatus = collectionStatus;
    if (tableUUID == null) {
      isInformation = true;
    } else {
      isInformation = false;
      this.table = database.getMetadata().getTable(tableUUID);
      getDenormalizeConfigurationFile();
    }
  }

  /**
   * Check if exist a configuration file for this database, if exist use to
   */
  private void getDenormalizeConfigurationFile() {
    loading.setVisible(true);
    if (collectionStatus.getDenormalizations()
      .contains(ViewerConstants.DENORMALIZATION_STATUS_PREFIX + table.getUuid())) {
      ConfigurationService.Util.call((DenormalizeConfiguration response) -> {
        denormalizeConfiguration = response;
        init();
      }).getDenormalizeConfigurationFile(database.getUuid(), table.getUuid());
    } else {
      denormalizeConfiguration = new DenormalizeConfiguration(database.getUuid(), table);
      init();
    }
  }

  private void init() {
    denormalizeConfigurationList.put(table.getUuid(), denormalizeConfiguration);
    loading.setVisible(false);

    Alert alert = new Alert(Alert.MessageAlertType.INFO, messages.dataTransformationTextForAlertColumnsOrder(), true,
      FontAwesomeIconManager.DATABASE_INFORMATION);
    content.add(alert);

    // root table
    TableNode parentNode = new TableNode(database, table);
    parentNode.setupChildren();
    ViewerTable table = parentNode.getTable();
    content.add(createRootTableCard(table));

    // relationship list
    FlowPanel relationShipPanel = new FlowPanel();
    relationShipPanel.setStyleName("data-transformation-panel");
    relationShipPanel.add(expandLevel(parentNode));
    content.add(relationShipPanel);

    createButtons();
  }

  private void createButtons() {
    btnClearConfiguration = new Button();
    btnClearConfiguration.setText(messages.basicActionClear());
    btnClearConfiguration.addStyleName("btn btn-times-circle btn-danger");
    btnClearConfiguration.setEnabled(true);
    btnClearConfiguration.addClickHandler(event -> {
      Window.Location.reload();
    });

    btnSaveConfiguration.setEnabled(true);
    btnSaveConfiguration.setText(messages.basicActionSave());
    btnSaveConfiguration.setStyleName("btn btn-save");
    btnSaveConfiguration.addClickHandler(clickEvent -> {
      DataTransformationUtils.saveConfiguration(database.getUuid(), denormalizeConfiguration);
      btnRunConfiguration.setEnabled(true);
    });

    btnSaveAllConfiguration.setEnabled(true);
    btnSaveAllConfiguration.setText("Save All");
    btnSaveAllConfiguration.setStyleName("btn btn-save");
    btnSaveAllConfiguration.addClickHandler(clickEvent -> {
      for (Map.Entry<String, DenormalizeConfiguration> entry : denormalizeConfigurationList.entrySet()) {
        DataTransformationUtils.saveConfiguration(database.getUuid(), entry.getValue());
      }
      btnRunConfiguration.setEnabled(true);
    });

    btnRunConfiguration.setEnabled(true);
    btnRunConfiguration.setStyleName("btn btn-run");
    btnRunConfiguration.addClickHandler(clickEvent -> {
      JobService.Util.call((Boolean result) -> {
      }).denormalizeTableJob(database.getUuid(), table.getUuid());
      HistoryManager.gotoJobs();
    });

    btnRunAllConfiguration.setEnabled(true);
    btnRunAllConfiguration.setText("Run All");
    btnRunAllConfiguration.setStyleName("btn btn-run");
    btnRunAllConfiguration.addClickHandler(clickEvent -> {
      JobService.Util.call((List<String> jobs) -> {
      }).denormalizeCollectionJob(database.getUuid());
      HistoryManager.gotoJobs();
    });

    String btnSaveText = "run";
    if (table != null) {
      btnSaveText = table.getName();
    }
    btnRunConfiguration.setText(btnSaveText);

    updateControllerPanel();
  }

  /**
   * Creates the root card of relationship tree
   *
   * @param table
   * @return BootstrapCard
   */
  private BootstrapCard createRootTableCard(ViewerTable table) {
    BootstrapCard card = new BootstrapCard();
    card.setTitleIcon(FontAwesomeIconManager.getTag(FontAwesomeIconManager.TABLE));
    card.setTitle(table.getId());
    card.setDescription(table.getDescription());
    rootTable = TransformationTable.getInstance(database, table, denormalizeConfiguration);
    FlowPanel rootTablePanel = new FlowPanel();
    rootTablePanel.add(rootTable);
    card.addExtraContent(rootTablePanel);
    return card;
  }

  /**
   * Creates the child card of relationship tree
   *
   * @param childNode
   * @return BootstrapCard
   */
  private FlowPanel createChildTableCard(TableNode childNode) {
    FlowPanel panel = new FlowPanel();
    panel.addStyleName("data-transformation-wrapper");

    FlowPanel grandChild = new FlowPanel();
    grandChild.addStyleName("data-transformation-child");

    ViewerTable childTable = childNode.getTable();
    BootstrapCard card = new BootstrapCard();

    card.setTitleIcon(FontAwesomeIconManager.getTag(FontAwesomeIconManager.TABLE));
    card.setTitle(childTable.getId());
    card.setDescription(childTable.getDescription());
    card.addExtraContent(getInformationAboutRelashionship(childNode));

    FlowPanel container = new FlowPanel();
    TransformationChildTables tableInstance = TransformationChildTables.getInstance(childNode, denormalizeConfiguration,
      rootTable, btnSaveConfiguration);
    MultipleSelectionTablePanel selectTable = tableInstance.createTable();

    SwitchBtn switchBtn = new SwitchBtn("Enable", false);
    switchBtn.setClickHandler(event -> {
      switchBtn.getButton().setValue(!switchBtn.getButton().getValue(), true); // workaround for ie11
      if (switchBtn.getButton().getValue()) {
        grandChild.add(expandLevel(childNode));
        DataTransformationUtils.includeRelatedTable(childNode, denormalizeConfiguration);
        container.add(selectTable);
      } else {
        DataTransformationUtils.removeRelatedTable(childNode, denormalizeConfiguration);
        grandChild.clear();
        container.clear();
        rootTable.redrawTable();
        btnSaveConfiguration.setEnabled(true);
      }
    });

    FlowPanel switchPanel = new FlowPanel();
    switchPanel.add(switchBtn);

    card.addHideContent(container, switchPanel);

    panel.add(card);
    panel.add(grandChild);

    if (denormalizeConfiguration != null) {
      for (RelatedTablesConfiguration relatedTable : denormalizeConfiguration.getRelatedTables()) {
        if (relatedTable.getUuid().equals(childNode.getUuid())) {
          switchBtn.getButton().setValue(true, true);
          card.setHideContentVisible(true);
          grandChild.add(expandLevel(childNode));
          container.add(selectTable);
          break;
        }
      }
    }

    return panel;
  }

  /**
   *
   * @param node
   * @return
   */
  private FlowPanel expandLevel(TableNode node) {
    FlowPanel relationShipList = new FlowPanel();
    for (Map.Entry<ViewerForeignKey, TableNode> entry : node.getChildren().entrySet()) {
      TableNode childNode = entry.getValue();

      childNode.setParentNode(node, entry.getKey());
      childNode.setupChildren();

      relationShipList.add(createChildTableCard(childNode));
    }
    return relationShipList;
  }

  private FlowPanel getInformationAboutRelashionship(TableNode node) {
    FlowPanel information = new FlowPanel();
    ViewerForeignKey foreignKey = node.getForeignKey();
    ViewerTable referencedTable = node.getParentNode().getTable();
    ViewerTable sourceTable = node.getTable();

    for (ViewerReference reference : foreignKey.getReferences()) {
      if (foreignKey.getReferencedTableUUID().equals(referencedTable.getUuid())) {
        ViewerColumn column = sourceTable.getColumns().get(reference.getSourceColumnIndex());
        information.add(buildReferenceInformation(
          messages.dataTransformationTextForIsRelatedTo(referencedTable.getId(), column.getDisplayName())));
      } else {
        ViewerColumn column = referencedTable.getColumns().get(reference.getSourceColumnIndex());
        information.add(buildReferenceInformation(
          messages.dataTransformationTextForIsReferencedBy(referencedTable.getId(), column.getDisplayName())));
      }
    }

    return information;
  }

  /**
   * Create a reference information panel
   *
   * @param message
   * @return FlowPanel
   */
  private FlowPanel buildReferenceInformation(SafeHtml message) {
    FlowPanel referenceInformation = new FlowPanel();
    referenceInformation.setStyleName("reference-panel");

    // icon
    HTML referenceIcon = new HTML(
      SafeHtmlUtils.fromSafeConstant(FontAwesomeIconManager.getTag(FontAwesomeIconManager.REFERENCE)));
    referenceIcon.setStyleName("icon");
    referenceInformation.add(referenceIcon);
    referenceInformation.add(new HTML(message));

    return referenceInformation;
  }

  private void updateControllerPanel() {
    if (!isInformation) {
      List<Button> buttons = new ArrayList<>();
      buttons.add(btnSaveAllConfiguration);
      buttons.add(btnSaveConfiguration);
      buttons.add(btnRunConfiguration);
      buttons.add(btnRunAllConfiguration);
      buttons.add(btnClearConfiguration);
      sidebar.updateControllerPanel(buttons);
    } else {
      sidebar.updateControllerPanel(null);
    }
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    if (database != null) {
      updateControllerPanel();
    }
  }
}