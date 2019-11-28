package com.databasepreservation.server.client.browse;

import com.databasepreservation.common.client.common.breadcrumb.BreadcrumbPanel;
import com.databasepreservation.common.client.common.RightPanel;
import com.databasepreservation.common.client.tools.BreadcrumbManager;
import com.databasepreservation.server.client.browse.upload.SIARDUpload;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class UploadPanel extends RightPanel {
  interface NewUploadPanelUiBinder extends UiBinder<Widget, UploadPanel> {
  }

  private static UploadPanel instance = null;

  public static UploadPanel getInstance() {
    if (instance == null) {
      instance = new UploadPanel();
    }
    return instance;
  }

  private static NewUploadPanelUiBinder uiBinder = GWT.create(NewUploadPanelUiBinder.class);

  @UiField
  TabPanel tabPanel;

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private UploadPanel() {
    initWidget(uiBinder.createAndBindUi(this));
    init();
  }

  /**
   * Uses BreadcrumbManager to show available information in the breadcrumbPanel
   *
   * @param breadcrumb
   *          the BreadcrumbPanel for this database
   */
  @Override
  public void handleBreadcrumb(BreadcrumbPanel breadcrumb) {
    BreadcrumbManager.updateBreadcrumb(breadcrumb, BreadcrumbManager.forNewUpload());
  }

  private void init() {
    tabPanel.add(SIARDUpload.getInstance(), messages.uploadPanelTextForTabUploadSIARDFile());
    tabPanel.selectTab(0);
  }


}
