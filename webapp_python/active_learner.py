from os.path import join, abspath
from post import Post, Prediction
import simplejson as json
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import Perceptron
import numpy as np
import collections

class active_learner(object):
	def __init__(self):
		self.classification = collections.OrderedDict()
		self.load_posts()
		self.load_classification()

	def load_posts(self):
		self.posts = []
		with open(join(abspath("../n2o_data"), "linked_in_posts.csv")) as f:
			for line in f:
				id, title, text, _, _, _, _, _, category, _, _ = line.replace("\\,", "<komma>").replace("\"", "").replace("\\", "").split(",")
				title = title.replace("<komma>", ",")
				text = text.replace("<komma>", ",")
				self.posts.append(Post(id,title,text))

	def load_other_classification_files(self):
		pass

	def load_classification(self):
		with open('data/classification.json') as infile:
			self.classification = json.JSONDecoder(object_pairs_hook=collections.OrderedDict).decode(infile.read())

	def save_classification(self):
		with open('data/classification.json', 'w') as outfile:
			json.dump(self.classification, outfile, indent = 2)

	def tag_demand(self, post_id, is_demand):
		self.classification[post_id] = {"demand": is_demand}
		self.save_classification()

	def tag_category(self, post_id, category):
		self.classification[post_id]["category"] = category
		self.save_classification()

	def not_enough_posts_tagged(self):
		numberOfDemandPosts = sum([1 if self.classification[each]["demand"]=="demand" else 0 for each in self.classification])
		numberOfNoDemandPosts = len(self.classification)-numberOfDemandPosts
		print numberOfDemandPosts, numberOfNoDemandPosts
		if numberOfDemandPosts>0 and numberOfNoDemandPosts>0:
			return False
		return True

	def post(self, post_id):
		posts = [post for post in self.posts if post.id == post_id]
		classifier, data, _ = self.build_classifier(posts)
		return zip(posts, self.calculate_predictions(classifier, data))

	def build_classifier(self, unlabeled_posts = None):
		labeled_posts  =  [post for post in self.posts if post.id in self.classification and self.classification[post.id]['demand'] and self.classification[post.id]['demand']!='noIdea']
		if unlabeled_posts is None:
			unlabeled_posts = [post for post in self.posts if not (post.id in self.classification and self.classification[post.id]['demand'])]
		X_train   = [post.data for post in labeled_posts]
		X_predict = [post.data for post in unlabeled_posts]
		Y_train = [self.classification[post.id]['demand'] for post in labeled_posts]

		# Build vectorizer
		vectorizer = TfidfVectorizer(sublinear_tf = True, max_df = 0.5, stop_words = 'english')
		X_train   = vectorizer.fit_transform(X_train)
		X_predict = vectorizer.transform(X_predict)
		Y_train   = np.array(Y_train)

		# Train the classifier
		classifier = Perceptron(n_iter = 50)
		classifier.fit(X_train, Y_train)
		return classifier, X_predict, unlabeled_posts

	def determine_uncertain_posts(self):
		if self.not_enough_posts_tagged():
			print "Choosing random posts"
			return [(post, Prediction()) for post in np.random.choice(self.posts, 5, False)]

		print "Choosing uncertain posts"

		classifier, X_predict, unlabeled_posts = self.build_classifier()
		predictions = self.calculate_predictions(classifier, X_predict)

		low_confidence_predictions = sorted(predictions, key = lambda prediction: prediction.conf)
		low_confidence_predictions = low_confidence_predictions[:10]

		return [(unlabeled_posts[pred.index], pred) for pred in low_confidence_predictions]

	def calculate_predictions(self, classifier, data):
		confidences = np.abs(classifier.decision_function(data))
		# The following line first sorts the confidences, and then extracts the predictions from these orders.
		# The index for the highest confidence is in the last position.
		# We then build Prediction objects for these.
		predictions = [Prediction(classifier.classes_[confOrders[-1]], confidences[index][confOrders[-1]], index) for (index, confOrders) in enumerate(np.argsort(confidences))]
		return predictions

	def determine_certain_posts(self):
		if self.not_enough_posts_tagged():
			return []

		classifier, X_predict, unlabeled_posts = self.build_classifier()
		predictions = self.calculate_predictions(classifier, X_predict)

		confidence_predictions = sorted(predictions, key = lambda prediction: prediction.conf)
		high_confidence_predictions = confidence_predictions[-10:]

		return [(unlabeled_posts[pred.index], pred) for pred in high_confidence_predictions]


	def damand_labeled_posts(self):
		demand_labled_posts = [post for post in self.posts if post.id in self.classification and self.classification[post.id]['demand']] 
		return [(post, self.classification[post.id]['demand']) for post in demand_labled_posts]


