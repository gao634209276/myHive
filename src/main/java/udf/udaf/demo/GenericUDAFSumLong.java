package udf.udaf.demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.*;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.util.StringUtils;

/**
 * 本类对于源码进行简单解析
 * Created by noah on 17-5-15.
 *
 * @see org.apache.hadoop.hive.ql.udf.generic.GenericUDAFSum.GenericUDAFSumLong
 */
public class GenericUDAFSumLong extends GenericUDAFEvaluator {
	static final Log LOG = LogFactory.getLog(org.apache.hadoop.hive.ql.udf.generic.GenericUDAFSum.class.getName());

	private PrimitiveObjectInspector inputOI;
	private LongWritable result;

	//这个方法返回了UDAF的返回类型，这里确定了sum自定义函数的返回类型是Long类型
	@Override
	public ObjectInspector init(Mode m, ObjectInspector[] parameters) throws HiveException {
		assert (parameters.length == 1);
		super.init(m, parameters);
		result = new LongWritable(0);
		inputOI = (PrimitiveObjectInspector) parameters[0];
		return PrimitiveObjectInspectorFactory.writableLongObjectInspector;
	}

	/**
	 * 存储sum的值的类
	 */
	static class SumLongAgg implements AggregationBuffer {
		boolean empty;
		long sum;
	}

	//创建新的聚合计算的需要的内存，用来存储mapper,combiner,reducer运算过程中的相加总和。

	@Override
	public AggregationBuffer getNewAggregationBuffer() throws HiveException {
		SumLongAgg result = new SumLongAgg();
		reset(result);
		return result;
	}
	//mapreduce支持mapper和reducer的重用，所以为了兼容，也需要做内存的重用。

	@Override
	public void reset(AggregationBuffer agg) throws HiveException {
		SumLongAgg myagg = (SumLongAgg) agg;
		myagg.empty = true;
		myagg.sum = 0;
	}

	private boolean warned = false;

	//map阶段调用，只要把保存当前和的对象agg，再加上输入的参数，就可以了。
	@Override
	public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
		assert (parameters.length == 1);
		try {
			merge(agg, parameters[0]);
		} catch (NumberFormatException e) {
			if (!warned) {
				warned = true;
				LOG.warn(getClass().getSimpleName() + " "
						+ StringUtils.stringifyException(e));
			}
		}
	}

	//mapper结束要返回的结果，还有combiner结束返回的结果
	@Override
	public Object terminatePartial(AggregationBuffer agg) throws HiveException {
		return terminate(agg);
	}

	//combiner合并map返回的结果，还有reducer合并mapper或combiner返回的结果。
	@Override
	public void merge(AggregationBuffer agg, Object partial) throws HiveException {
		if (partial != null) {
			SumLongAgg myagg = (SumLongAgg) agg;
			myagg.sum += PrimitiveObjectInspectorUtils.getLong(partial, inputOI);
			myagg.empty = false;
		}
	}

	//reducer返回结果，或者是只有mapper，没有reducer时，在mapper端返回结果。
	@Override
	public Object terminate(AggregationBuffer agg) throws HiveException {
		SumLongAgg myagg = (SumLongAgg) agg;
		if (myagg.empty) {
			return null;
		}
		result.set(myagg.sum);
		return result;
	}
}