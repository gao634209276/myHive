package udf.generic;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDFUtils;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDFUtils.ReturnObjectInspectorResolver;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;

@Description(name = "nvl", value = "_FUNC_(value,default_value) - Returns default value if value"
		+ " is null else returns values", extended = "Example:\n"
		+ "  > SELECT _FUNC_(null,'bla') FROM src LIMIT 1;")
public class GenericUDFNvl extends GenericUDF {
	// select nvl(1,2) as col1,nvl (null, 5) as col2,nvl(null,"STUFF") as col3
	// from src limit 1;

	private ObjectInspector[] argumentOIs;
	private ReturnObjectInspectorResolver returnOIResolver;

	/**
	 * ObjectInspector:处理数据库你类型到java类型的映射,单例
	 * ReturnObjectInspectorResolver : 当输入参数有多个,而每个参数的类型不同时,使这些参数转换类型,产生相同的ObjectInspector
	 */
	@Override
	public ObjectInspector initialize(ObjectInspector[] arguments)// 使得不同类型的参数产生同一java类型的映射
			throws UDFArgumentException {
		argumentOIs = arguments;
		if (arguments.length != 2) {
			throw new UDFArgumentLengthException(
					"The operator 'NVL' accepts 2 arguments");
		}
		// 当多个参数的ObjectInspector不同时,是否转换成一致的
		returnOIResolver = new GenericUDFUtils.ReturnObjectInspectorResolver(true);
		if (!(returnOIResolver.update(arguments[0]) && returnOIResolver.update(arguments[1]))) {
			throw new UDFArgumentTypeException(2,
					"The 1st and 2nd args of function 'NVL' should have the same type, "
							+ "but they are different: \""
							+ arguments[0].getTypeName() + "\" and \""
							+ arguments[1].getTypeName() + "\"");
		}
		return returnOIResolver.get();
	}

	@Override
	public Object evaluate(DeferredObject[] arguments) throws HiveException {
		// 用ObjectInspector转化数据库类型为java类型
		Object retVal = returnOIResolver.convertIfNecessary(arguments[0].get(), argumentOIs[0]);
		if (retVal == null) {
			retVal = returnOIResolver.convertIfNecessary(arguments[1].get(),
					argumentOIs[0]);
		}
		return retVal;
	}

	@Override
	public String getDisplayString(String[] children) {
		StringBuilder sb = new StringBuilder();
		sb.append("if ");
		sb.append(children[0]);
		sb.append(" is null ");
		sb.append("returns");
		sb.append(children[1]);
		return sb.toString();
	}

}
