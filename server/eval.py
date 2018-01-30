import sys
import os
import tensorflow as tf
import numpy as np
from math import sqrt
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 

# ----------------------------------------------------------
# Example usage:
# python eval.py models/model.ckpt data/testdata.npy
# ----------------------------------------------------------



# Network Parameters
n_hidden_1 = 64 # 1st layer number of features
n_hidden_2 = 200 # 2nd layer number of features
n_hidden_3 = 200
n_hidden_4 = 256
n_input = 36
n_classes = 1



# tf Graph input
inLayer = tf.placeholder(tf.float32, [None, n_input])

# Create model
def multilayer_perceptron(weights, biases):
    # Hidden layer with RELU activation
    layer_1 = tf.add(tf.matmul(inLayer, weights['h1']), biases['b1'])
    layer_1 = tf.nn.relu(layer_1)

    # Hidden layer with RELU activation
    layer_2 = tf.add(tf.matmul(layer_1, weights['h2']), biases['b2'])
    layer_2 = tf.nn.relu(layer_2)

    # Hidden layer with RELU activation
    layer_3 = tf.add(tf.matmul(layer_2, weights['h3']), biases['b3'])
    layer_3 = tf.nn.relu(layer_3)

    # Hidden layer with RELU activation
    layer_4 = tf.add(tf.matmul(layer_3, weights['h4']), biases['b4'])
    layer_4 = tf.nn.relu(layer_4)

    # Output layer with linear activation
    out_layer = tf.matmul(layer_4, weights['out']) + biases['out']
    return out_layer

# Store layers weight & bias
weights = {
    'h1': tf.Variable(tf.random_normal([n_input, n_hidden_1], 0, 0.1)),
    'h2': tf.Variable(tf.random_normal([n_hidden_1, n_hidden_2], 0, 0.1)),
    'h3': tf.Variable(tf.random_normal([n_hidden_2, n_hidden_3], 0, 0.1)),
    'h4': tf.Variable(tf.random_normal([n_hidden_3, n_hidden_4], 0, 0.1)),
    'out': tf.Variable(tf.random_normal([n_hidden_4, n_classes], 0, 0.1))
}
biases = {
    'b1': tf.Variable(tf.random_normal([n_hidden_1], 0, 0.1)),
    'b2': tf.Variable(tf.random_normal([n_hidden_2], 0, 0.1)),
    'b3': tf.Variable(tf.random_normal([n_hidden_3], 0, 0.1)),
    'b4': tf.Variable(tf.random_normal([n_hidden_4], 0, 0.1)),
    'out': tf.Variable(tf.random_normal([n_classes], 0, 0.1))
}


saver = tf.train.Saver()

network = multilayer_perceptron(weights, biases)

testdata = np.load(sys.argv[2])

rowCount = testdata.shape[0]


x, y = testdata[:, 0:36], testdata[:, 36:37]

errorSum = 0.0

histogramSums = {}

# Launch the graph
print("Sample Prediction    Real value     Square Error")

with tf.Session() as sess:
    saver.restore(sess, sys.argv[1])
    for n in range(rowCount):
        isValue = network.eval(session=sess, feed_dict={inLayer:[list(x[n,:])]})[0,0]
        shouldValue = y[n,0]
        mse = (isValue-shouldValue)**2
        errorSum += mse
        histogramBucket = min(int(shouldValue / 3600), 24)
        if not histogramBucket in histogramSums:
            histogramSums[histogramBucket] = {'count': 1, 'sum': mse}
        else:
            histogramSums[histogramBucket]['count']+=1
            histogramSums[histogramBucket]['sum']+=mse
        print("{: 6d} {: 7.2f}     {: 7.2f}      {: 9.2f}".format(n,isValue,shouldValue,mse))

print("Remaining time (hours)     Count     Error     sqrt(Error)")
for bucket in sorted(list(histogramSums)):
    c = histogramSums[bucket]['count']
    s = histogramSums[bucket]['sum']
    print("{: 26d} {: 9d} {: 9.2f} {: 9.2f}".format(bucket,c,(s/c),sqrt(s/c)))

print("Average mean square error: {} = {}²".format(errorSum/rowCount, sqrt(errorSum/rowCount) ))
print("All results in seconds")
