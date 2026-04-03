package com.example.glue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultMemberAccessTest {
    public interface I1A {}
    public interface I1B {}
    public interface I1 extends I1A, I1B {}
    public interface I2A {}
    public interface I2B {}
    public interface I2 extends I2A, I2B {}

    public class GrandParent implements I1 {}
    public class Parent extends GrandParent implements I2 {}
    public class Child extends Parent implements I2 {}

    private final MemberAccess memberAccess = new DefaultMemberAccess();

    @Test
    public void testGetHierarchy() {
        List<Class<?>> childHierarchy = new ArrayList<>();
        List<Class<?>> parentHierarchy = new ArrayList<>();
        List<Class<?>> grandParentHierarchy = new ArrayList<>();

        memberAccess.getHierarchy(Child.class).forEach(childHierarchy::add);
        memberAccess.getHierarchy(Parent.class).forEach(parentHierarchy::add);
        memberAccess.getHierarchy(GrandParent.class).forEach(grandParentHierarchy::add);

        assertThat(grandParentHierarchy)
                .containsExactly(GrandParent.class, Object.class, I1.class, I1A.class, I1B.class);
        assertThat(parentHierarchy)
                .containsExactly(Parent.class, GrandParent.class, I2.class, Object.class, I1.class, I2A.class, I2B.class, I1A.class, I1B.class);
        assertThat(childHierarchy)
                .containsExactly(Child.class, Parent.class, I2.class, GrandParent.class, I2A.class, I2B.class, Object.class, I1.class, I1A.class, I1B.class);
    }
}