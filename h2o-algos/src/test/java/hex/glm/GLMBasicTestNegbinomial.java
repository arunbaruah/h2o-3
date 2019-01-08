package hex.glm;

import hex.DataInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import water.*;
import water.fvec.Frame;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class GLMBasicTestNegbinomial extends TestUtil {

  @BeforeClass
  public static void setup() { stall_till_cloudsize(1); }

  // Make sure all three implementations of ginfo computation in GLM get the same results
  @Test
  public void testGradientTask() {
    GLMModel model;
    Frame tfr;

    DataInfo dinfo = null;
    try {
      Scope.enter();
      tfr = parse_test_file("smalldata/jira/pubdev_1839_repro_train.csv");
      Scope.track(tfr);
      
      GLMModel.GLMParameters params = new GLMModel.GLMParameters(GLMModel.GLMParameters.Family.negativebinomial, 
              GLMModel.GLMParameters.Family.negativebinomial.defaultLink, new double[]{0}, new double[]{0}, 0, 0);

      params._train = tfr._key;
      params._lambda = new double[]{0};
      params._use_all_factor_levels = true;

      dinfo = new DataInfo(tfr, null, 1, 
              params._use_all_factor_levels || params._lambda_search, params._standardize ? DataInfo.TransformType.STANDARDIZE : DataInfo.TransformType.NONE, DataInfo.TransformType.NONE, true, false, false, false, false, false);
      DKV.put(dinfo._key,dinfo);
      Scope.track_generic(dinfo);
      double [] beta = MemoryManager.malloc8d(dinfo.fullN()+1);
      Random rnd = new Random(987654321);
      for (int i = 0; i < beta.length; ++i)
        beta[i] = 1 - 2 * rnd.nextDouble();

      GLMTask.GLMGradientTask grtSpc = new GLMTask.GLMBinomialGradientTask(null,dinfo, params, params._lambda[0], beta).doAll(dinfo._adaptedFrame);
      GLMTask.GLMGradientTask grtGen = new GLMTask.GLMGenericGradientTask(null,dinfo, params, params._lambda[0], beta).doAll(dinfo._adaptedFrame);
      for (int i = 0; i < beta.length; ++i)
        assertEquals("gradients differ", grtSpc._gradient[i], grtGen._gradient[i], 1e-4);
      params = new GLMModel.GLMParameters(GLMModel.GLMParameters.Family.gaussian, GLMModel.GLMParameters.Family.gaussian.defaultLink, new double[]{0}, new double[]{0}, 0, 0);
      params._use_all_factor_levels = false;
      dinfo.remove();
      dinfo = new DataInfo(tfr, null, 1, params._use_all_factor_levels || params._lambda_search, params._standardize ? DataInfo.TransformType.STANDARDIZE : DataInfo.TransformType.NONE, DataInfo.TransformType.NONE, true, false, false, false, false, false);
      DKV.put(dinfo._key,dinfo);
      beta = MemoryManager.malloc8d(dinfo.fullN()+1);
      rnd = new Random(1987654321);
      for (int i = 0; i < beta.length; ++i)
        beta[i] = 1 - 2 * rnd.nextDouble();
      grtSpc = new GLMTask.GLMGaussianGradientTask(null,dinfo, params, params._lambda[0], beta).doAll(dinfo._adaptedFrame);
      grtGen = new GLMTask.GLMGenericGradientTask(null,dinfo, params, params._lambda[0], beta).doAll(dinfo._adaptedFrame);
      for (int i = 0; i < beta.length; ++i)
        assertEquals("gradients differ: " + Arrays.toString(grtSpc._gradient) + " != " + Arrays.toString(grtGen._gradient), grtSpc._gradient[i], grtGen._gradient[i], 1e-4);
      dinfo.remove();
    } finally {
      Scope.exit();
      if (dinfo != null) dinfo.remove();
    }
  }


// test and compare mojo/pojo/predict values



}
