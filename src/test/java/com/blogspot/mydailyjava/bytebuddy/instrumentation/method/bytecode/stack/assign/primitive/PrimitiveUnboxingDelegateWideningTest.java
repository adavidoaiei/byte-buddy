package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveUnboxingDelegateWideningTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Short.class, long.class, "shortValue", "()S", Opcodes.I2L, 1, 1},
                {Short.class, float.class, "shortValue", "()S", Opcodes.I2F, 0, 0},
                {Short.class, double.class, "shortValue", "()S", Opcodes.I2D, 1, 1},
                {Integer.class, long.class, "intValue", "()I", Opcodes.I2L, 1, 1},
                {Integer.class, float.class, "intValue", "()I", Opcodes.I2F, 0, 0},
                {Integer.class, double.class, "intValue", "()I", Opcodes.I2D, 1, 1},
                {Long.class, float.class, "longValue", "()J", Opcodes.L2F, 0, 1},
                {Long.class, double.class, "longValue", "()J", Opcodes.L2D, 1, 1},
                {Float.class, double.class, "floatValue", "()F", Opcodes.F2D, 1, 1},
        });
    }

    private final Class<?> primitiveType;
    private final Class<?> referenceType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;
    private final int wideningOpcode;
    private final int sizeChange;
    private final int interimMaximum;

    public PrimitiveUnboxingDelegateWideningTest(Class<?> referenceType,
                                                 Class<?> primitiveType,
                                                 String unboxingMethodName,
                                                 String unboxingMethodDescriptor,
                                                 int wideningOpcode,
                                                 int sizeChange,
                                                 int interimMaximum) {
        this.primitiveType = primitiveType;
        this.referenceType = referenceType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
        this.wideningOpcode = wideningOpcode;
        this.sizeChange = sizeChange;
        this.interimMaximum = interimMaximum;
    }

    @Mock
    private TypeDescription referenceTypeDescription, primitiveTypeDescription;
    @Mock
    private Assigner chainedAssigner;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(referenceTypeDescription.represents(referenceType)).thenReturn(true);
        when(primitiveTypeDescription.isPrimitive()).thenReturn(true);
        when(primitiveTypeDescription.represents(primitiveType)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(chainedAssigner);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testTrivialBoxing() throws Exception {
        StackManipulation stackManipulation = PrimitiveUnboxingDelegate.forReferenceType(referenceTypeDescription)
                .assignUnboxedTo(primitiveTypeDescription, chainedAssigner, false);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(interimMaximum));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(referenceType),
                unboxingMethodName,
                unboxingMethodDescriptor);
        verify(methodVisitor).visitInsn(wideningOpcode);
        verifyNoMoreInteractions(methodVisitor);
    }
}
