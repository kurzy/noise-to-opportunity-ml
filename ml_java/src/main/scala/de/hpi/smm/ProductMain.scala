package de.hpi.smm

import java.io.{FileWriter, File}
import java.util.Random
import de.hpi.smm.Constants._
import com.blog_intelligence.nto.Document
import de.hpi.smm.classification.ProductClassifier
import de.hpi.smm.data_reader.DataReader
import weka.classifiers.`lazy`.IBk
import weka.classifiers.bayes.{NaiveBayesMultinomial, NaiveBayes}
import weka.classifiers.evaluation.output.prediction.PlainText
import weka.classifiers.functions.{MultilayerPerceptron, SMO, Logistic, VotedPerceptron}
import weka.classifiers.{Classifier, Evaluation}
import weka.classifiers.trees.J48
import weka.core.{Utils, DenseInstance, Attribute, Instances}
import scala.collection.mutable
import scala.collection.JavaConverters._

object ProductMain {

	val dataReader = new DataReader(
		new File("../n2o_data/linked_in_posts.csv"),
		new File("../n2o_data/brochures.csv"),
		new File(CLASSIFICATION_JSON))

	var posts = mutable.ArrayBuffer[Document]()
	var brochures = mutable.ArrayBuffer[Document]()

	def readData(): Unit = {
		posts = mutable.ArrayBuffer[Document]()
		brochures = mutable.ArrayBuffer[Document]()

		if (dataReader != null) {

			dataReader.readPostsLinewise { post =>
				posts += post
			}("category")

			dataReader.readBrochuresLinewise(List("en")) { brochure =>
				brochures += brochure
			}
		}
	}

	val groupSizes  = List(6)
	val classifiers = List(
		new MultilayerPerceptron()
		, new Logistic
		, new SMO()
	)
	val binaryFeatures = List(false)
	val normalize = List(false)

	def main(args: Array[String]): Unit = {
		readData()

		groupSizes.foreach { groupSize =>
			classifiers.foreach { classifier =>
				binaryFeatures.foreach { useBinaryFeature =>
					normalize.foreach { normalizeFeatures =>
						println(f"groupSize:$groupSize, classifier:${classifier.getClass},binaryFeature:$useBinaryFeature,normalize:$normalizeFeatures")

						val analyzer = new ProductClassifier(brochures.toList,groupSize,classifier,useBinaryFeature,normalizeFeatures)
						analyzer.buildTrainInstances()
						analyzer.buildClassifier()

						analyzer.printValidation(posts.toList)
					}
				}
			}
		}
	}

	def writeBestPredictions(): Unit = {
//		val result = mutable.Map[Document, Array[Double]]()
//
//		analyzer.dataReader.readPostsLinewise { doc =>
//			result(doc) = analyzer.distributionForInstance(doc)
//		}("category")
//
//		for (i <- 0 to analyzer.wordCountWithTfIdf.size) {
//			val f = new FileWriter(new File(s"../ml_java/${analyzer.classAttr.value(i)}.csv"))
//			result.toArray.filter { case (doc, distribution) =>
//				distribution(i) == distribution.max
//			}.sortBy(-_._2(i)).take(100).foreach { case (doc, distribution) =>
//				f.write(doc.id + s": ${distribution(i)}" + "\n" + doc.wholeText + "\n\n")
//			}
//			f.close()
//		}
	}

}