package dev.cerus.visualcrafting.api.math;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class MatrixMath {

    public static final Matrix4f TRANSLATION_CENTER = translation(0.5f, 0.5f, 0.5f);
    public static final Matrix4f TRANSLATION_RESET = translation(-0.5f, -0.5f, -0.5f);

    public static Matrix4f translation(final float tX, final float tY, final float tZ) {
        return m4FromArray(new float[][] {
                new float[] {1f, 0f, 0f, tX},
                new float[] {0f, 1f, 0f, tY},
                new float[] {0f, 0f, 1f, tZ},
                new float[] {0f, 0f, 0f, 1f}
        }).assume(Matrix4fc.PROPERTY_AFFINE);
    }

    public static Matrix4f scale(final float tX, final float tY, final float tZ) {
        return m4FromArray(new float[][] {
                new float[] {tX, 0f, 0f, 0f},
                new float[] {0f, tY, 0f, 0f},
                new float[] {0f, 0f, tZ, 0f},
                new float[] {0f, 0f, 0f, 1f}
        }).assume(Matrix4fc.PROPERTY_AFFINE);
    }

    public static Matrix3f rotationX(final float t) {
        return m3FromArray(new float[][] {
                new float[] {1f, 0f, 0f},
                new float[] {0f, (float) Math.cos(t), (float) -Math.sin(t)},
                new float[] {0f, (float) Math.sin(t), (float) Math.cos(t)}
        });
    }

    public static Matrix3f rotationY(final float t) {
        return m3FromArray(new float[][] {
                new float[] {(float) Math.cos(t), 0f, (float) Math.sin(t)},
                new float[] {0f, 1f, 0f},
                new float[] {(float) -Math.sin(t), 0f, (float) Math.cos(t)}
        });
    }

    public static Matrix3f rotationZ(final float t) {
        return m3FromArray(new float[][] {
                new float[] {(float) Math.cos(t), (float) -Math.sin(t), 0f},
                new float[] {(float) Math.sin(t), (float) Math.cos(t), 0f},
                new float[] {0f, 0f, 1f}
        });
    }

    public static Matrix4f combine(final Matrix4f... matrices) {
        Matrix4f result = null;
        for (final Matrix4f matrix : matrices) {
            if (result == null) {
                result = new Matrix4f(matrix);
            } else {
                result.mul(matrix);
            }
        }
        return result;
    }

    public static Matrix4f combineAndExpand(final Matrix3f... matrices) {
        Matrix3f result = null;
        for (final Matrix3f matrix : matrices) {
            if (result == null) {
                result = new Matrix3f(matrix);
            } else {
                result.mul(matrix);
            }
        }

        final Matrix4f m4 = new Matrix4f().assume(Matrix4fc.PROPERTY_AFFINE);
        m4.set(result);
        return m4;
    }

    public static Matrix3f m3FromArray(final float[][] arr) {
        final Matrix3f m = new Matrix3f();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                m.set(c, r, arr[r][c]);
            }
        }
        return m;
    }

    public static Matrix4f m4FromArray(final float[][] arr) {
        final Matrix4f m = new Matrix4f();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                m.set(c, r, arr[r][c]);
            }
        }
        return m;
    }

    public static Matrix4f newAffineM4() {
        return new Matrix4f().assume(Matrix4fc.PROPERTY_AFFINE);
    }
}