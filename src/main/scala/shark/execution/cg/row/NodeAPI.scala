package shark.execution.cg.row

import org.apache.hadoop.io.Writable

import org.apache.hadoop.hive.serde2.typeinfo.StructTypeInfo
import org.apache.hadoop.hive.serde2.typeinfo.ListTypeInfo
import org.apache.hadoop.hive.serde2.typeinfo.MapTypeInfo
import org.apache.hadoop.hive.serde2.typeinfo.UnionTypeInfo
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo
import org.apache.hadoop.hive.serde2.typeinfo.{ TypeInfoFactory => TIF }

import org.apache.hadoop.hive.serde2.objectinspector.ConstantObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.MapObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.UnionObjectInspector
import org.apache.hadoop.hive.serde2.objectinspector.{ ObjectInspector => OI }
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory
import org.apache.hadoop.hive.serde2.objectinspector.primitive.{ PrimitiveObjectInspectorFactory => POIF }
import org.apache.hadoop.hive.serde2.objectinspector.{ ObjectInspectorFactory => OIF }

import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils
import org.apache.hadoop.hive.ql.exec.FunctionRegistry

import shark.execution.cg.SetDeferred
import shark.execution.cg.SetRaw
import shark.execution.cg.SetWritable
import shark.execution.cg.CGAssertRuntimeException

object TypeUtil {
	private val map = scala.collection.mutable.Map[TypeInfo, DataType]()

	val NullType = new CGNull(POIF.writableVoidObjectInspector, null, -1)
	val StringType = new CGPrimitiveString(POIF.writableStringObjectInspector, null, -1)
	val BinaryType = new CGPrimitiveBinary(POIF.writableBinaryObjectInspector, null, -1)
	val IntegerType = new CGPrimitiveInt(POIF.writableIntObjectInspector, null, -1)
	val BooleanType = new CGPrimitiveBoolean(POIF.writableBooleanObjectInspector, null, -1)
	val FloatType = new CGPrimitiveFloat(POIF.writableFloatObjectInspector, null, -1)
	val DoubleType = new CGPrimitiveDouble(POIF.writableDoubleObjectInspector, null, -1)
	val LongType = new CGPrimitiveLong(POIF.writableLongObjectInspector, null, -1)
	val ByteType = new CGPrimitiveByte(POIF.writableByteObjectInspector, null, -1)
	val ShortType = new CGPrimitiveShort(POIF.writableShortObjectInspector, null, -1)
	val TimestampType = new CGPrimitiveTimestamp(POIF.writableTimestampObjectInspector, null, -1)

	register(NullType)
	register(StringType)
	register(BinaryType)
	register(IntegerType)
	register(BooleanType)
	register(FloatType)
	register(DoubleType)
	register(LongType)
	register(ByteType)
	register(ShortType)
	register(TimestampType)

	// TODO need to support the non-primitive data type, which may require creating the new
	// object inspector(union / struct)
	def register(dt: DataType) {
		map += (dt.typeInfo -> dt)
	}

	def getSetWritableClass(): Class[_] = classOf[SetWritable]
	def getDeferredObjectClass(): Class[_] = classOf[SetDeferred]

	def getDataType(ti: TypeInfo): DataType = map.get(ti) match {
		case Some(x) => x
		case None => throw new CGAssertRuntimeException("Cannot mapping to the Datatype")
	}

	def getTypeInfo(oi: OI): TypeInfo = TypeInfoUtils.getTypeInfoFromObjectInspector(oi)

	def getDataType(oi: OI): DataType = if (oi.isInstanceOf[PrimitiveObjectInspector])
		getDataType(getTypeInfo(oi))
	else
		CGField.create(oi, null)
		
	def dtToString(dt: DataType): String = {
		dt match {
			case TypeUtil.BinaryType => "PrimitivePrimitiveObjectInspectorFactory.writableBinaryObjectInspector"
			case TypeUtil.BooleanType => "PrimitiveObjectInspectorFactory.writableBooleanObjectInspector"
			case TypeUtil.ByteType => "PrimitiveObjectInspectorFactory.writableByteObjectInspector"
			case TypeUtil.DoubleType => "PrimitiveObjectInspectorFactory.writableDoubleObjectInspector"
			case TypeUtil.FloatType => "PrimitiveObjectInspectorFactory.writableFloatObjectInspector"
			case TypeUtil.IntegerType => "PrimitiveObjectInspectorFactory.writableIntObjectInspector"
			case TypeUtil.LongType => "PrimitiveObjectInspectorFactory.writableLongObjectInspector"
			case TypeUtil.ShortType => "PrimitiveObjectInspectorFactory.writableShortObjectInspector"
			case TypeUtil.StringType => "PrimitiveObjectInspectorFactory.writableStringObjectInspector"
			case TypeUtil.TimestampType => "PrimitiveObjectInspectorFactory.writableTimestampObjectInspector"
			case _ => throw new CGAssertRuntimeException("couldn't support the data type:" + dt.clazz)
		}
	}
}

// TODO the TreeNode API was from catalyst, will merge catalyst in the near future
abstract class TreeNode[BaseType <: TreeNode[BaseType]] {
  self: BaseType with Product =>

  /** Returns a Seq of the children of this node */
  def children: Seq[BaseType]
}

/**
 * A [[TreeNode]] with no children.
 */
trait LeafNode[BaseType <: TreeNode[BaseType]] {
  def children = Nil
}

/**
 * A [[TreeNode]] with a single [[child]].
 */
trait UnaryNode[BaseType <: TreeNode[BaseType]] {
  def child: BaseType
  def children = child :: Nil
}

abstract class ExprNode[NodeType <: TreeNode[NodeType]] extends TreeNode[NodeType] {
	self: NodeType with Product =>
}
