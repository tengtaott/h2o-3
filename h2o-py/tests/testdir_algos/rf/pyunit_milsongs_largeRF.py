import sys, shutil
sys.path.insert(1, "../../../")
import h2o, tests
import random

def milsong_checkpoint(ip,port):

    milsong_train = h2o.upload_file(h2o.locate("bigdata/laptop/milsongs/milsongs-train.csv.gz"))
    milsong_valid = h2o.upload_file(h2o.locate("bigdata/laptop/milsongs/milsongs-test.csv.gz"))

    # build first model
    ntrees1 = random.sample(range(50,100),1)[0]
    max_depth1 = random.sample(range(2,6),1)[0]
    min_rows1 = random.sample(range(10,16),1)[0]
    print "ntrees model 1: {0}".format(ntrees1)
    print "max_depth model 1: {0}".format(max_depth1)
    print "min_rows model 1: {0}".format(min_rows1)
    model1 = h2o.random_forest(x=milsong_train[1:],y=milsong_train[0],ntrees=ntrees1,max_depth=max_depth1, min_rows=min_rows1,
                               validation_x=milsong_valid[1:],validation_y=milsong_valid[0],seed=1234)

    # save the model, then load the model
    model_path = h2o.save_model(model1,force=True)
    restored_model = h2o.load_model(model_path)
    shutil.rmtree(model_path)

    # continue building the model
    ntrees2 = ntrees1 + 50
    max_depth2 = max_depth1
    min_rows2 = min_rows1
    print "ntrees model 2: {0}".format(ntrees2)
    print "max_depth model 2: {0}".format(max_depth2)
    print "min_rows model 2: {0}".format(min_rows2)
    model2 = h2o.random_forest(x=milsong_train[1:],y=milsong_train[0],ntrees=ntrees2,max_depth=max_depth2, min_rows=min_rows2,
                               validation_x=milsong_valid[1:],validation_y=milsong_valid[0],
                               checkpoint=restored_model._id,seed=1234)

    # build the equivalent of model 2 in one shot
    model3 = h2o.random_forest(x=milsong_train[1:],y=milsong_train[0],ntrees=ntrees2,max_depth=max_depth2, min_rows=min_rows2,
                               validation_x=milsong_valid[1:],validation_y=milsong_valid[0],seed=1234)

    assert isinstance(model2,type(model3))
    assert model2.mse(valid=True)==model3.mse(valid=True), "Expected Model 2 MSE: {0} to be the same as Model 4 MSE: {1}".format(model2.mse(valid=True), model3.mse(valid=True))

if __name__ == "__main__":
    tests.run_test(sys.argv, milsong_checkpoint)
