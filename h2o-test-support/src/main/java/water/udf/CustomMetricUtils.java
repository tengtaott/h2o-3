package water.udf;

import hex.*;
import org.junit.Assert;
import water.DKV;
import water.Key;
import water.TestUtil;
import water.fvec.Frame;
import water.udf.metric.MEACustomMetric;
import water.util.FrameUtils;

import java.io.IOException;
import java.util.Arrays;

import static water.udf.JFuncUtils.loadTestFunc;

public class CustomMetricUtils {

    static void testNullModelRegression(final CFuncRef func) throws Exception {
        final Frame f = TestUtil.Datasets.iris();
        Frame pred = null; Model model = null;
        try {
            NullModelParameters params = new NullModelParameters() {{
                _train = f._key;
                _response_column = "sepal_len";
                _custom_metric_func = func.toRef();
            }};
            model = new NullModelBuilder(params).trainModel().get();
            pred = model.score(f, null, null, true, func);
            Assert.assertEquals("Null model generates only a single model metrics",
                1, model._output.getModelMetrics().length);
            ModelMetrics mm = model._output.getModelMetrics()[0].get();
            Assert.assertEquals("Custom model metrics should compute mean of response column",
                f.vec("sepal_len").mean(), mm._custom_metric.value, 1e-8);
        } finally {
            FrameUtils.delete(f, pred, model);
            DKV.remove(func.getKey());
        }
    }

    static class NullModelOutput extends Model.Output {

        public NullModelOutput(ModelBuilder b) {
            super(b);
        }
    }
    
    static class NullModelParameters extends Model.Parameters {
        @Override public String fullName() { return "nullModel"; }
        @Override public String algoName() { return "nullModel"; }
        @Override public String javaName() { return NullModelBuilder.class.getName(); }
        @Override public long progressUnits() { return 1; }
    }

    static class NullModel extends Model<NullModel, NullModelParameters, NullModelOutput> {
        public NullModel(Key<NullModel> selfKey, NullModelParameters parms, NullModelOutput output) {
            super(selfKey, parms, output);
        }
        @Override
        public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
            switch(_output.getModelCategory()) {
                case Binomial:    return new ModelMetricsBinomial.MetricBuilderBinomial(domain);
                case Multinomial: return new ModelMetricsMultinomial.MetricBuilderMultinomial(_output.nclasses(), domain, _parms._auc_type);
                case Regression:  return new ModelMetricsRegression.MetricBuilderRegression();
                default: return null;
            }
        }
        @Override
        protected double[] score0(double[] data, double[] preds) {
            Arrays.fill(preds, 0);
            return preds;
        }
    }
    
    static class NullModelBuilder extends ModelBuilder<NullModel, NullModelParameters, NullModelOutput> {
        public NullModelBuilder(NullModelParameters parms) {
            super(parms);
            init(false);
        }

        @Override
        public void init(boolean expensive) {
            super.init(expensive);
        }

        @Override
        protected Driver trainModelImpl() {
            return new Driver() {
                @Override
                public void computeImpl() {
                    init(true);
                    NullModel model = new NullModel(dest(), _parms, new NullModelOutput(NullModelBuilder.this));
                    try {
                        model.delete_and_lock(_job);
                    } finally {
                        model.unlock(_job);
                    }
                }
            };
        }

        @Override
        public ModelCategory[] can_build() {
            return new ModelCategory[]{
                ModelCategory.Regression,
                ModelCategory.Binomial,
                ModelCategory.Multinomial,
            };
        }

        @Override
        public boolean isSupervised() {
            return true;
        }
    }

}
