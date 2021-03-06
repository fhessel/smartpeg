#based on: https://stackoverflow.com/questions/38399609/tensorflow-deep-neural-network-for-regression-always-predict-same-results-in-one


from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import get_data

import tensorflow as tf
from tensorflow.contrib import learn
#import matplotlib.pyplot as plt

from sklearn.pipeline import Pipeline
from sklearn import datasets, linear_model
from sklearn import cross_validation
import numpy as np

import argparse

parser = argparse.ArgumentParser(description='Network for the prediction of the left time, the laundry has to dry.')
parser.add_argument('--lr', type=float, dest='learning_rate', action='store', default=0.001, help='learning rate of the adam optimiser')
parser.add_argument('--epochs', type=int, dest='training_epochs', action='store', default=100, help='number of epochs, the networkshall be trained')
parser.add_argument('--batch', type=int, dest='batch_size', action='store', default=50, help='number of training examples ,that shall be trained in one batch')
parser.add_argument('--display', type=int, dest='display_step', action='store', default=1, help='frequency of prints')
parser.add_argument('--evaluate', dest='model_path', action='store', default='None', help='evaluates the given model')
parser.add_argument('--refine', dest='refinement_path', action='store', default='None', help='refines the given model')
parser.add_argument('--data', dest='data_path', action='store', default='data/HDC1080.npy', help='the path, where the data is stored')
parser.add_argument('--save', dest='save_path', action='store', default='models/new.ckpt', help='the path, where the model is saved')


args = parser.parse_args()

data = get_data.get_data_from_file(args.data_path)
x, y = data[:, 0:36], data[:, 36:37]
X_train, X_test, Y_train, Y_test = cross_validation.train_test_split(
	x, y, test_size=0.2, random_state=42)

# Parameters
learning_rate = args.learning_rate #0.001
training_epochs = args.training_epochs #500
batch_size = args.batch_size #10
display_step = args.display_step #1
# Network Parameters
n_hidden_1 = 64 # 1st layer number of features
n_hidden_2 = 200 # 2nd layer number of features
n_hidden_3 = 200
n_hidden_4 = 256
n_input = X_train.shape[1]
n_classes = 1

total_len = X_train.shape[0]
total_batch = int(total_len/batch_size)


# tf Graph input
x = tf.placeholder("float", [None, n_input])
y = tf.placeholder("float", [None, 1])

# Create model
def multilayer_perceptron(x, weights, biases):
    # Hidden layer with RELU activation
    layer_1 = tf.add(tf.matmul(x, weights['h1']), biases['b1'])
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

# Construct model
pred = multilayer_perceptron(x, weights, biases)

# Define loss and optimizer
cost = tf.reduce_mean(tf.square(pred-y))
optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate).minimize(cost)

saver = tf.train.Saver()


# Launch the graph
with tf.Session() as sess:
	sess.run(tf.initialize_all_variables())

	if args.refinement_path!='None':
		saver.restore(sess, args.refinement_path)

	min_cost = float('inf')

	if args.model_path == 'None':
		# Training cycle
		for epoch in range(training_epochs):
			avg_cost = 0.
			# Loop over all batches
			for i in range(total_batch-1):
				batch_x = X_train[i*batch_size:(i+1)*batch_size]
				batch_y = Y_train[i*batch_size:(i+1)*batch_size]
				# Run optimization op (backprop) and cost op (to get loss value)
				_, c, p = sess.run([optimizer, cost, pred],
								   feed_dict={x: batch_x, y: batch_y})
				# Compute average loss
				avg_cost += c / total_batch

			# Display logs per epoch step
			if epoch % display_step == 0:
				print ("Epoch:", '%04d' % (epoch+1), "cost=", \
					"{:.9f}".format(avg_cost))
				print ("[*]----------------------------")
				for i in range(3):
					print ("label value:", batch_y[i], \
						"estimated value:", p[i])
				print ("[*]============================")
			error = cost.eval({x: X_test, y: Y_test})
			if error < min_cost:
				min_cost = error
				saver.save(sess, args.save_path)
		print ("Optimization Finished!")
	else:
		saver.restore(sess, args.model_path)
		# Test model
		error = cost.eval({x: X_test, y: Y_test})
		print("Mean squared error:", error)

