package de.hpi.smm.classification

import com.blog_intelligence.nto.{Document, ProductClassification, RawDocument}
import de.hpi.smm.classification.helper.NoneClassifier
import de.hpi.smm.data_reader.DataReader
import de.hpi.smm.nlp.NLP
import weka.classifiers.functions.{Logistic, MultilayerPerceptron}
import weka.classifiers.{Classifier, Evaluation}
import weka.core.{Attribute, DenseInstance, Instances, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable

class ProductClassifier(
		originalBrochures: Seq[Document],
		nlp: NLP,
		groupSize: Int = 6,
		originalClassifier: Classifier = new Logistic(),
		binaryFeatures: Boolean = true,
		normalize: Boolean = true,
		useNoneClassifier: Boolean = false
	) extends Serializable {
	val PRINT_FEATURE_WORDS = false

	val classifier = if (useNoneClassifier) new NoneClassifier(originalClassifier) else originalClassifier

	var wordCountWithTfIdf = mutable.Map[String, mutable.Map[String, Double]]()

	var featureAttributes: java.util.ArrayList[Attribute] = null
	var featureWords: Map[String, Int] = null
	var classAttr: Attribute = null

	private var brochures = originalBrochures

	init()

	private def init() = {
		val wordCount = mutable.Map[String, mutable.Map[String, Int]]()
		val documentCount = mutable.Map[String, Int]().withDefaultValue(0)
		var N = 0

		brochures = brochures.flatMap { doc =>
			var id = 0
			doc.sentences.grouped(groupSize).map { sentences =>
				id += 1
				Document(s"${doc.id}-$id", "", sentences.mkString(" "), sentences, doc.documentClass)
			}
		}
		brochures.foreach { doc =>
			val docClass = doc.documentClass

			if (!wordCount.contains(docClass))
				wordCount(docClass) = mutable.Map[String, Int]().withDefaultValue(0)
			doc.textTokens.foreach { word =>
				wordCount(docClass)(word) += 1
			}
			doc.textTokens.toSet[String].foreach { word =>
				documentCount(word) += 1
			}
			N += 1
		}


		wordCountWithTfIdf = wordCount.map { case (className, counts) =>
			(className, counts.map { case (word, count) =>
				(word, count.toDouble * Math.log(N.toDouble / documentCount(word).toDouble))
			})
		}

		featureWords = determineFeatures(wordCountWithTfIdf).zipWithIndex.toMap
		if (PRINT_FEATURE_WORDS)
			println(featureWords)
		val classes = new java.util.ArrayList[String](wordCountWithTfIdf.keySet.asJava)
		if (!classes.contains("None"))
			classes.add("None")
		classAttr = new Attribute("@@class@@", classes)

		featureAttributes = new java.util.ArrayList[Attribute](featureWords.keys.map(new Attribute(_)).asJavaCollection)
		featureAttributes.add(classAttr)
	}

	private def determineFeatures(wordCounts: mutable.Map[String, mutable.Map[String, Double]]): Array[String] = {
		var result = mutable.Set[String]()
		wordCounts.foreach { case (className, counts) =>
			counts.toList.sortBy(-_._2).take(10).foreach { case (word, _) =>
				result += word
			}
		}
		result.toArray
	}

	private def constructFeatureValues(doc: Document): Array[Double] = {
		val result = new Array[Double](featureWords.size + 1)
		doc.textTokens.foreach { word =>
			if(featureWords.contains(word)) {
				if(binaryFeatures){
					result(featureWords(word)) = 1.0
				}
				else{
					result(featureWords(word)) += 1.0
				}
			}
		}
		result(result.size - 1) = classAttr.indexOfValue(doc.documentClass)
		result
	}

	private def normalize(features: Array[Double]): Array[Double] = {
		if(!normalize)
			return features
		val lastIndex = features.size - 1
		val instanceClass = features(lastIndex)
		features(lastIndex) = 0.0
		if (features.sum == 0.0) {
			features(lastIndex) = instanceClass
			return features
		}
		Utils.normalize(features)
		features(lastIndex) = instanceClass
		features
	}

	def buildTrainInstances() : Instances = {
		val trainInstances = new Instances("train", featureAttributes, featureAttributes.size())
		trainInstances.setClassIndex(featureAttributes.size() - 1)
		brochures.foreach { doc =>
			val features = constructFeatureValues(doc)
			trainInstances.add(new DenseInstance(1.0, normalize(features)))
		}
		trainInstances
	}

	def buildClassifier(): Unit = {
		val trainInstances = buildTrainInstances()
		classifier.buildClassifier(trainInstances)
	}

	private def buildTestInstances(posts: Seq[Document]): Instances = {
		val testInstances = new Instances("test", featureAttributes, featureAttributes.size())
		testInstances.setClassIndex(featureAttributes.size() - 1)
		posts.foreach { doc =>
			val features = constructFeatureValues(doc)
      testInstances.add(new DenseInstance(1.0, normalize(features)))
		}
		testInstances
	}

	def validate(posts: Seq[Document]): Evaluation = {
		val testInstances = buildTestInstances(posts)
		val evaluation = new Evaluation(testInstances)
		evaluation.evaluateModel(classifier, testInstances)
		evaluation
	}

	def predict(text: String): List[ProductClassification] = {
		val id = ""
		val title = ""

		val rawPost = RawDocument(id, title, text, null)
		val sentences = nlp.detectSentences(rawPost)
		val post = Document(id, title, text, sentences, "None")

		val instance = new DenseInstance(1.0, constructFeatureValues(post))
		val dummyInstances = new Instances("bla", featureAttributes,1)
		dummyInstances.setClassIndex(featureAttributes.size() - 1)
		instance.setDataset(dummyInstances)
		val distribution = classifier.distributionForInstance(instance)

		distribution.zipWithIndex.map { case (probability,index) =>
			val className = classAttr.value(index)
			ProductClassification(className,probability)
		}.toList
	}
}
