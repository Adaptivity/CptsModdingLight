package kovukore.coloredlights.src.asm.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import cpw.mods.fml.common.FMLLog;
import kovukore.coloredlights.src.asm.transformer.core.ASMUtils;
import kovukore.coloredlights.src.asm.transformer.core.MethodTransformer;

/**
 * Fields added to net.minecraft.world.chunk.storage.ExtendedBlockStorage:
 *   NibbleArray rColorArray
 *   NibbleArray gColorArray
 *   NibbleArray bColorArray
 *   
 * Methods added to net.minecraft.world.chunk.storage.ExtendedBlockStorage:
 *   setRedColorArray
 *   setGreenColorArray
 *   setBlueColorArray
 *   getRedColorArray
 *   getGreenColorArray
 *   getBlueColorArray
 * 
 * Methods modified on net.minecraft.world.chunk.storage.ExtendedBlockStorage:
 *   setExtBlocklightValue
 *   getExtBlocklightValue
 * 
 * @author Josh
 *
 */
public class TransformExtendedBlockStorage extends MethodTransformer {
	
	private boolean addedFields = false;
	private FieldNode rColorArray;
	private FieldNode gColorArray;
	private FieldNode bColorArray;
	private FieldNode blockLSBArray;
	
	private boolean addedMethods = false;
			
	@Override
	protected boolean transforms(ClassNode clazz, MethodNode method) {
		
		return method.name.equals("setExtBlocklightValue") | method.name.equals("getExtBlocklightValue") | method.name.equals("<init>");
	}

	@Override
	protected boolean transform(ClassNode clazz, MethodNode method) {
		
		boolean changed = false;
		
		if (!addedFields)
		{
			addRGBNibbleArrays(clazz);
			changed = true;
		}
						
		if (method.name.equals("setExtBlocklightValue"))
		{
			transformSetExtBlocklightValue(clazz, method);
			changed = true;
		}

		if (method.name.equals("getExtBlocklightValue"))
		{
			transformGetExtBlocklightValue(clazz, method);
			changed = true;
		}

		if (method.name.equals("<init>"))
		{
			transformConstructor(clazz, method);
			changed = true;
		}
		
		return changed;
	}
	
	@Override
	protected boolean postTransformClass(ClassNode clazz)
	{
		if (!addedMethods)
		{
			addRGBNibbleArrayMethods(clazz);
			return true;
		}			
		else
			return false;
	}

	@Override
	protected boolean transforms(String className) {
		return className.equals("net.minecraft.world.chunk.storage.ExtendedBlockStorage");
	}

	private void addRGBNibbleArrays(ClassNode clazz)
	{
		Type typeNibbleArray = Type.getType(net.minecraft.world.chunk.NibbleArray.class);
		
		for (FieldNode f : clazz.fields)
			if (f.name.equals("blockLSBArray"))
				blockLSBArray = f;
		
		rColorArray = new FieldNode(Opcodes.ACC_PUBLIC, "rColorArray", typeNibbleArray.getDescriptor(), null, null);
		gColorArray = new FieldNode(Opcodes.ACC_PUBLIC, "gColorArray", typeNibbleArray.getDescriptor(), null, null);
		bColorArray = new FieldNode(Opcodes.ACC_PUBLIC, "bColorArray", typeNibbleArray.getDescriptor(), null, null);
		
		clazz.fields.add(rColorArray);
		clazz.fields.add(gColorArray);
		clazz.fields.add(bColorArray);			
		
		addedFields = true;
	}
	
	private boolean addRGBNibbleArrayMethods(ClassNode clazz)
	{
		if (addedFields && !addedMethods)
		{
			// These new methods are required for storing/loading the new nibble arrays to disk
						
			clazz.methods.add(ASMUtils.generateSetterMethod(clazz.name, "setRedColorArray", rColorArray.name, rColorArray.desc));
			clazz.methods.add(ASMUtils.generateSetterMethod(clazz.name, "setGreenColorArray", gColorArray.name, gColorArray.desc));
			clazz.methods.add(ASMUtils.generateSetterMethod(clazz.name, "setBlueColorArray", bColorArray.name, bColorArray.desc));

			clazz.methods.add(ASMUtils.generateGetterMethod(clazz.name, "getRedColorArray", rColorArray.name, rColorArray.desc));
			clazz.methods.add(ASMUtils.generateGetterMethod(clazz.name, "getGreenColorArray", gColorArray.name, gColorArray.desc));
			clazz.methods.add(ASMUtils.generateGetterMethod(clazz.name, "getBlueColorArray", bColorArray.name, bColorArray.desc));
			
			addedMethods = true;
		}
		
		return addedMethods;
	}
	
	private void transformConstructor(ClassNode clazz, MethodNode m)
	{
		String ebsInternalName = clazz.name;
		Type typeNibbleArray = Type.getType(net.minecraft.world.chunk.NibbleArray.class);
				
		// Initializes array the same length as blockLSBArray:
//	    18  aload_0 [this]
// 	    19  new net.minecraft.world.chunk.NibbleArray [4]
// 	    22  dup
//		23  aload_0 [this]
//	    24  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.blockLSBArray : byte[] [3]
//	    27  arraylength
//	    28  iconst_4
//	    29  invokespecial net.minecraft.world.chunk.NibbleArray(int, int) [5]
//	    32  putfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.blockMetadataArray : net.minecraft.world.chunk.NibbleArray [6]
		
		// Remove the return, to be re-inserted later
		
		AbstractInsnNode returnNode = ASMUtils.findLastReturn(m);
		
		if (returnNode == null)
		{
			FMLLog.severe("Failed to find RETURN statement on %s/%s %s", clazz.name, m.name, m.desc);
		}
		else
			m.instructions.remove(returnNode);
						
		// Initialize rColorArray
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		m.instructions.add(new TypeInsnNode(Opcodes.NEW, typeNibbleArray.getInternalName()));
		m.instructions.add(new InsnNode(Opcodes.DUP));		
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, blockLSBArray.name, blockLSBArray.desc));
		m.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
		m.instructions.add(new InsnNode(Opcodes.ICONST_4));
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, typeNibbleArray.getInternalName(), "<init>", "(II)V"));
		m.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, ebsInternalName, rColorArray.name, rColorArray.desc));

		// Initialize gColorArray
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		m.instructions.add(new TypeInsnNode(Opcodes.NEW, typeNibbleArray.getInternalName()));
		m.instructions.add(new InsnNode(Opcodes.DUP));		
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, blockLSBArray.name, blockLSBArray.desc));
		m.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
		m.instructions.add(new InsnNode(Opcodes.ICONST_4));
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, typeNibbleArray.getInternalName(), "<init>", "(II)V"));
		m.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, ebsInternalName, gColorArray.name, gColorArray.desc));
	
		// Initialize bColorArray
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		m.instructions.add(new TypeInsnNode(Opcodes.NEW, typeNibbleArray.getInternalName()));
		m.instructions.add(new InsnNode(Opcodes.DUP));		
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, blockLSBArray.name, blockLSBArray.desc));
		m.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
		m.instructions.add(new InsnNode(Opcodes.ICONST_4));
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, typeNibbleArray.getInternalName(), "<init>", "(II)V"));
		m.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, ebsInternalName, bColorArray.name, bColorArray.desc));
		
		m.instructions.add(returnNode);		
	}

	private void transformSetExtBlocklightValue(ClassNode clazz, MethodNode m)
	{
		String ebsInternalName = clazz.name;
		Type typeNibbleArray = Type.getType(net.minecraft.world.chunk.NibbleArray.class);
		
		// Already in stock method:
//		 0  aload_0 [this]
//		 1  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.blocklightArray : net.minecraft.world.chunk.NibbleArray [91]
//		 4  iload_1 [x]
//		 5  iload_2 [y]
//		 6  iload_3 [z]
//		 7  iload 4 [lightValue]
//		 9  invokevirtual net.minecraft.world.chunk.NibbleArray.set(int, int, int, int) : void [93]
		// return is there by default - remove for now
				
		AbstractInsnNode oldReturn = ASMUtils.findLastReturn(m);
		
		if (oldReturn != null)
			m.instructions.remove(oldReturn);
		
		// Colored light mod: Store red value		
//		12  aload_0 [this]
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//		13  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.rColorArray : net.minecraft.world.chunk.NibbleArray [98]
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, rColorArray.name, rColorArray.desc));
//		16  iload_1 [x]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
//		17  iload_2 [y]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
//		18  iload_3 [z]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
//		19  iload 4 [lightValue]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
//		21  iconst_5
		m.instructions.add(new InsnNode(Opcodes.ICONST_5));
//		22  ishr
		m.instructions.add(new InsnNode(Opcodes.ISHR));
//		23  bipush 15
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 15));
//		25  iand
		m.instructions.add(new InsnNode(Opcodes.IAND));
//		26  invokevirtual net.minecraft.world.chunk.NibbleArray.set(int, int, int, int) : void [93]
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, typeNibbleArray.getInternalName(), "set", "(IIII)V"));
		
		// Colored light mod: Store green value
//		29  aload_0 [this]
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//		30  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.gColorArray : net.minecraft.world.chunk.NibbleArray [100]
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, gColorArray.name, gColorArray.desc));
//		33  iload_1 [x]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
//		34  iload_2 [y]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
//		35  iload_3 [z]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
//		36  iload 4 [lightValue]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
//		38  bipush 10
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 10));
//		40  ishr
		m.instructions.add(new InsnNode(Opcodes.ISHR));
//		41  bipush 15
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 15));
//		43  iand
		m.instructions.add(new InsnNode(Opcodes.IAND));
//		44  invokevirtual net.minecraft.world.chunk.NibbleArray.set(int, int, int, int) : void [93]
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, typeNibbleArray.getInternalName(), "set", "(IIII)V"));
		
		// Colored light mod: Store blue value
//		47  aload_0 [this]
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//		48  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.bColorArray : net.minecraft.world.chunk.NibbleArray [102]
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, bColorArray.name, bColorArray.desc));
//		51  iload_1 [x]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
//		52  iload_2 [y]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
//		53  iload_3 [z]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
//		54  iload 4 [lightValue]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
//		56  bipush 15
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 15));
//		58  ishr
		m.instructions.add(new InsnNode(Opcodes.ISHR));
//		59  bipush 15
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 15));
//		61  iand
		m.instructions.add(new InsnNode(Opcodes.IAND));
//		62  invokevirtual net.minecraft.world.chunk.NibbleArray.set(int, int, int, int) : void [93]
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, typeNibbleArray.getInternalName(), "set", "(IIII)V"));
		
//		65  return
		m.instructions.add(new InsnNode(Opcodes.RETURN));
		
		
	}

	private void transformGetExtBlocklightValue(ClassNode clazz, MethodNode m)
	{
		String ebsInternalName = clazz.name;
		Type typeNibbleArray = Type.getType(net.minecraft.world.chunk.NibbleArray.class);
		
		// Already in stock method:
//       0  aload_0 [this]
//       1  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.blocklightArray : net.minecraft.world.chunk.NibbleArray [91]
//       4  iload_1 [par1]
//       5  iload_2 [par2]
//       6  iload_3 [par3]
//       7  invokevirtual net.minecraft.world.chunk.NibbleArray.get(int, int, int) : int [110]
		// ireturn is there by default - remove for now
		AbstractInsnNode returnNode = ASMUtils.findLastReturn(m);
		
		if (returnNode != null)
			m.instructions.remove(returnNode);
		
//      10  aload_0 [this]
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//      11  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.rColorArray : net.minecraft.world.chunk.NibbleArray [98]
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, rColorArray.name, rColorArray.desc));
//      14  iload_1 [par1]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
//      15  iload_2 [par2]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
//      16  iload_3 [par3]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
//      17  invokevirtual net.minecraft.world.chunk.NibbleArray.get(int, int, int) : int [110]
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, typeNibbleArray.getInternalName(), "get", "(III)I"));
//      20  iconst_5
		m.instructions.add(new InsnNode(Opcodes.ICONST_5));
//      21  ishl
		m.instructions.add(new InsnNode(Opcodes.ISHL));
//      22  ior
		m.instructions.add(new InsnNode(Opcodes.IOR));

//      23  aload_0 [this]
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//      24  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.gColorArray : net.minecraft.world.chunk.NibbleArray [100]
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, gColorArray.name, gColorArray.desc));
//      27  iload_1 [par1]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
//      28  iload_2 [par2]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
//      29  iload_3 [par3]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
//      30  invokevirtual net.minecraft.world.chunk.NibbleArray.get(int, int, int) : int [110]
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, typeNibbleArray.getInternalName(), "get", "(III)I"));
//      33  bipush 10
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 10));
//      35  ishl
		m.instructions.add(new InsnNode(Opcodes.ISHL));
//      36  ior
		m.instructions.add(new InsnNode(Opcodes.IOR));
		
//      37  aload_0 [this]
		m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//      38  getfield net.minecraft.world.chunk.storage.ExtendedBlockStorage.bColorArray : net.minecraft.world.chunk.NibbleArray [102]
		m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, ebsInternalName, bColorArray.name, bColorArray.desc));
//      41  iload_1 [par1]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
//      42  iload_2 [par2]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
//      43  iload_3 [par3]
		m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
//      44  invokevirtual net.minecraft.world.chunk.NibbleArray.get(int, int, int) : int [110]
		m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, typeNibbleArray.getInternalName(), "get", "(III)I"));
//      47  bipush 15
		m.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 15));
//      49  ishl
		m.instructions.add(new InsnNode(Opcodes.ISHL));
//      50  ior
		m.instructions.add(new InsnNode(Opcodes.IOR));
		
		if (returnNode != null)		
//      51  ireturn
		m.instructions.add(returnNode);		
	}	
	
}
