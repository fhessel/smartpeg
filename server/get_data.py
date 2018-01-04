import urllib.request
import numpy as np

def get_data_from_http():
	http = urllib.request.urlopen("http://smartpeg.fhessel.de/smartpeg/training/HDC1080")
	string = http.read().decode()
	http.close()
	examples = string.split('\n')

	for i in range(1, len(examples)):
		ex = examples[i].split(';')
		examples[i] = list(map(float, ex))

	examples = np.matrix(examples[1:len(examples)])
	return examples

def save_data(matrix, file_name):
	np.save(file_name, matrix)
	
def get_data_from_file(file_name):
	return np.load(file_name)

	
#print(examples.shape)

