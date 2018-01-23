import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.ParametersAreNonnullByDefault;

import crawler.HtmlCrawler;
import nvd.data.DBConnection;
import nvd.data.NvdItemDao;
import nvd.data.RawDataProcess;
import nvd.data.SummaryExtraction;
import nvd.model.ProductSearch;
import w2v.W2VRNN;
import weka.ModelTrain;
import weka.classifiers.trees.RandomForest;
import workflow.MainWorkFlow;

///// 安装运行过程
///  装MySql, Scala SDK, Java SDK
///  先在MySql里手动建表( 表名在 src\main\scala\nvd\data\NvdItemDao中找 )
///  苏琰梓用IDEA来编译运行, 新建Maven项目, 然后把杨开懋的项目import进来. 在src\main\java\下新建test文件夹, 将当前源代码test.java拷入

/**
 * 以下为代码整体结果:
 * 1. 连接NVD, 获得NVD中每一个软件对应的漏洞集,  漏洞集中每一个漏洞保存其(漏洞类型,漏洞严重程度,描述信息).  [相当于一个mapping]
 * 2. 输入: 工具1输出的漏洞集(已存入数据库)  输出: 对漏洞集的统计. 统计每一个软件的漏洞数量, 类型(每个软件中所有漏洞中数量最多的类型), 严重程度(每个软件中所有漏洞中严重程度最高等级). (3张数据库表)
 * 3. 手工操作, 选取特征词. 代码在 src\main\scala\lucene\LuceneUtils.scala 通过 getWordsFrequencyFromFile获得NVD的高频词, 然后手动选择特征词
 * 4. 由第三步的关键词和整体输入的软件名称,版本号,并结合搜索引擎的网址, 进入爬虫, 获得搜索页面的网址信息.
 * 5. 从第4步的网址,打开网页, 去标签处理后存储到数据库中.
 * 6. 从第5步的输入通过para2vec 做成向量 RNN; 第五步的结果存在 searchres 资料/MySQL/数据里 可以看到这个表
 * 7. 输入是第6步的向量和第二步的标准输出, 然后进行十倍交叉验证, 获得结果.
 */

///  杨开懋的原始驱动代码在src\main\scala\Test.scala    Scala是在java虚拟机上的另一个语言, 也是编译成.class,故而可以和Java混用.
///  这段代码苏琰梓用Java复现  除了2,3 其它都有, 第5步写简单了

/**
 * @author Zhang Yifan
 */
@ParametersAreNonnullByDefault
public final class TestJava {

  public static void main(String[] args) throws SQLException {

    ////  第一步:得到nvd漏洞信息
    Connection conn = DBConnection.getConnection();
    RawDataProcess rawDataProcess = new RawDataProcess();
    rawDataProcess.readData(conn);

    ////  第一步终止, rawDataProcess.readData的实现第一行, 可以定义数据爬取年份 src\main\scala\nvd\data\RawDataProcess.scala
    ///   第二步 select原始数据存入vul表中, 代码不在这里, 代码在 "资料/util.sql" 次序不是从前往后执行, 而是拷出来执行的, 其中insert vul_cwe这一段应该在insert vul_category之前调用. 只有前4段是构建第二步.
    ///   苏琰梓认为,CREATE TABLE `search_res这一段是第五步的内容.
    ///   第二步终止

    ///   第四步 没有给软件名和版本号, 而是把NVD里各个软件名和版本号结合这里给出的特征词全部爬一遍
    HtmlCrawler hc = new HtmlCrawler();
    hc.init();
    String string = "bed";               /// 特征词, 多个特征词用空格隔开, 连城长字符串即可
    String[] s = hc.getBingRes(string);  /// s数组 里是拿到的10个搜索网址
    //以下是把网址封装成ProductSearch对象, 存到数据库
    NvdItemDao nvdItemDao = new NvdItemDao(conn);
    for (String s1 : s) {
      String[] array = new String[] { s1 };
      ProductSearch productSearch = new ProductSearch(string, array);
      nvdItemDao.saveSearchRes(productSearch);
    }

    ////  第五步 目前仅仅爬了一个网页,crawlPage返回直接是去了标签的文字, 应该改成一个循环
    //     读取网页上的信息（去标签）
    //        https://www.bedbathandbeyond.com/
    System.out.print(hc.crawlPage("https://www.bedbathandbeyond.com/"));
    //        System.out.println("======");

    ////   获得词频, 可能和第三步有关.

    // Map<String, Object> map = LuceneUtils.getWordsFrequency2("I you I abc edf");
    //        System.out.print(map);

    ////   转成向量, 向量会写到 src\main\scala\nvd\data\SummaryExtraction.scala 56行 定义的位置data\\word2vec\\summary.pv
    W2VRNN.makeParVec();
    //
    SummaryExtraction summaryExtraction = new SummaryExtraction(conn);
    summaryExtraction.writeVectors("test");

    conn.close();

    ///   第七步,
    MainWorkFlow mainWorkFlow = new MainWorkFlow(conn);
    mainWorkFlow.getTrainData("impact", "test_vector", "remark");

    ModelTrain modelTrain = new ModelTrain();
    modelTrain.crossValidate2("impact", new RandomForest(), "remark");
  }

  @Deprecated
  private TestJava() { throw new UnsupportedOperationException(); }
}
