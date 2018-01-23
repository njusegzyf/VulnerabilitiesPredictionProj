package nvd.model

/**
  * Created by ReggieYang on 2016/10/22.
  */
case class NvdItem(id: String = "",
                   product: String = "",
                   impactScore: Double = 0d,
                   reference: String = "",
                   cwe: String = "",
                   summary: String = "") {

  def toSqlItem:String = "('" + id + "','" + product.replaceAll("'", "\\\\\'") + "'," + impactScore + ",'" + reference.replaceAll("'", "\\\\\'") +
  "','" + cwe + "','" + summary.replaceAll("'", "\\\\\'") + "')"

}
