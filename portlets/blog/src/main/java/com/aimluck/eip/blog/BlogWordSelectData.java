/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.blog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.blog.util.BlogUtils;
import com.aimluck.eip.cayenne.om.portlet.EipTBlogComment;
import com.aimluck.eip.cayenne.om.portlet.EipTBlogEntry;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * ブログエントリー検索ボックス用データです。
 * 
 */
public class BlogWordSelectData extends ALAbstractSelectData {
  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
      .getLogger(BlogWordSelectData.class.getName());

  /** 検索ワード */
  private ALStringField searchWord;

  private DataContext dataContext;

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#init(com.aimluck.eip.modules.actions.common.ALAction,
   *      org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
   */
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "name_kana");
    }

    dataContext = DatabaseOrmService.getInstance().getDataContext();

    super.init(action, rundata, context);
  }

  /**
   * 自分がオーナーのアドレスを取得
   * 
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectList(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected List selectList(RunData rundata, Context context) {
    List list;

    // ページャからきた場合に検索ワードをセッションへ格納する
    if (!rundata.getParameters().containsKey(ALEipConstants.LIST_START)
        && !rundata.getParameters().containsKey(ALEipConstants.LIST_SORT)) {
      ALEipUtils.setTemp(rundata, context, "Blogsword", rundata.getParameters()
          .getString("sword"));
    }

    // 検索ワードの設定
    searchWord = new ALStringField();
    searchWord.setTrim(true);
    // セッションから値を取得する。
    // 検索ワード未指定時は空文字が入力される
    searchWord.setValue(ALEipUtils.getTemp(rundata, context, "Blogsword"));

    try {
      list = searchList(rundata, context);
      if (list == null)
        list = new ArrayList();
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
    return buildPaginatedList(list);
  }

  /**
   * 未使用。
   * 
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectDetail(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected Object selectDetail(RunData rundata, Context context) {
    return null;
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultData(java.lang.Object)
   */
  protected Object getResultData(Object obj) {
    try {
      DataRow dataRow = (DataRow) obj;

      Integer entry_id = (Integer) ALEipUtils.getObjFromDataRow(dataRow,
          EipTBlogEntry.ENTRY_ID_PK_COLUMN);
      long ower_id = ((Integer) ALEipUtils.getObjFromDataRow(dataRow,
          EipTBlogEntry.OWNER_ID_COLUMN)).longValue();

      BlogEntryResultData rd = new BlogEntryResultData();
      rd.initField();
      rd.setEntryId(entry_id.longValue());
      rd.setOwnerId(ower_id);
      rd.setOwnerName(BlogUtils.getUserFullName((int) ower_id));

      rd.setTitle(ALCommonUtils.compressString((String) ALEipUtils
          .getObjFromDataRow(dataRow, EipTBlogEntry.TITLE_COLUMN),
          getStrLength()));
      rd.setNote(BlogUtils.compressString((String) ALEipUtils
          .getObjFromDataRow(dataRow, EipTBlogEntry.NOTE_COLUMN), 100));

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日（EE）");
      rd.setTitleDate(sdf.format((Date) ALEipUtils.getObjFromDataRow(dataRow,
          EipTBlogEntry.CREATE_DATE_COLUMN)));

      SelectQuery query = new SelectQuery(EipTBlogComment.class);
      Expression exp = ExpressionFactory.matchDbExp(
          EipTBlogComment.EIP_TBLOG_ENTRY_PROPERTY + "."
              + EipTBlogEntry.ENTRY_ID_PK_COLUMN, entry_id);
      query.setQualifier(exp);
      List list = dataContext.performQuery(query);
      if (list != null && list.size() > 0) {
        rd.setCommentsNum(list.size());
      }

      return rd;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 未使用。
   * 
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultDataDetail(java.lang.Object)
   */
  protected Object getResultDataDetail(Object obj) {
    return null;
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#getColumnMap()
   */
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();

    return map;
  }

  private List searchList(RunData rundata, Context context) {
    List list = null;
    try {
      String word = searchWord.getValue();

      if (word == null || word.length() == 0) {
        return new ArrayList();
      }

      // SQLの作成
      StringBuffer statement = new StringBuffer();
      statement
          .append("SELECT DISTINCT t0.entry_id, t0.owner_id, t0.title, t0.note, ");
      statement.append("t0.thema_id, t0.update_date, t0.create_date ");
      statement
          .append("FROM eip_t_blog_entry as t0 left join eip_t_blog_comment as t1 on t1.entry_id = t0.entry_id ");
      statement.append("WHERE  (t0.title LIKE '%" + word
          + "%') OR (t0.note LIKE '%" + word
          + "%')  OR (t0.entry_id = t1.entry_id AND (t1.comment LIKE '%" + word
          + "%')) ORDER BY t0.create_date DESC");
      String query = statement.toString();

      SQLTemplate rawSelect = new SQLTemplate(EipTBlogEntry.class, query, true);
      rawSelect.setFetchingDataRows(true);
      list = dataContext.performQuery(rawSelect);

    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList();
    }
    return list;
  }

  /**
   * 検索ワードを取得します。
   * 
   * @return
   */
  public ALStringField getSearchWord() {
    return searchWord;
  }

}
