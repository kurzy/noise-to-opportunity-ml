package de.hpi.smm.feature_extraction

import de.hpi.smm.domain.{GenericCountsCounter, Document}

import scala.collection.mutable

class FeatureExtractor(smooting: Boolean) {

	val genericCounter = new GenericCountsCounter()
	genericCounter.smoothing = smooting;

	val blacklist = Array(
		".", ",", ":", "-RRB-", "-LRB-", "$",
		// english
		"IN", "DT", "TO", "CC", "VBZ",
		// german
		"APP", "ART", "KO", "KO", "PP",
		"PR", "PT", "TRUNC", "VA", "VM", "VV"
	)

	def names: Array[String] = {
		val values = features.flatMap(_.name).toArray
		val names = new Array[String](values.size + 2)
		names(0) = "id"
		names(names.size - 1) = "CLASS"
		System.arraycopy(values, 0, names, 1, values.size)
		names
	}

	var documents = List[Document]()
	var features: List[Feature] = List()

	/**
	 * Demand posts often contain more questions than normal posts,
	 * especially many questions in a row sometimes
	 */
	def questionNumber(): FeatureExtractor = {
		addFeature(new QuestionNumberFeature())
		this
	}

	def questionWords(): FeatureExtractor = {
		addFeature(new QuestionWordsFeature())
		this
	}

	def addressTheReader(): FeatureExtractor = {
		addFeature(new AddressReaderFeature())
		this
	}

	def needNGrams(): FeatureExtractor = {
		addFeature(new NeedNGramsFeature())
		this
	}

	def containsEMail(): FeatureExtractor = {
		addFeature(new ContainsEMailFeature())
		this
	}

	/**
	 * Demand posts often contain imperative clauses like "help me", "share your information"
	 */
	def imperativeWords(): FeatureExtractor = {
		addFeature(new ImperativeNumberFeature())
		this
	}

	/**
	 * Captures common need words like "required", "need" etc.
	 */
	def needWords(clsName: String, thresholds: (Double, Double)): FeatureExtractor = {
		addFeature(new NeedWordFeature(genericCounter, clsName, thresholds))
		this
	}

	/**
	 * Captures common thank you notes at the end of a demand post
	 */
	def thankYou(): FeatureExtractor = {
		// TODO IMPLEMENT
		this
	}

	/**
	 * Captures common endings like "share your experiences", "please share your results"
	 */
	def share(): FeatureExtractor = {
		// TODO IMPLEMENT
		this
	}

	/**
	 * Captures the most common demand words from a BOW model
	 */
	def mostCommonWordsFromBOW(): FeatureExtractor = {
		// TODO IMPLEMENT
		this
	}

	/**
	 * Add a new posts to this feature builder, and store it for internal use.
	 */
	def touch(document: Document): Unit = {
		documents ::= document
		if (document.isClassified) {
			features.foreach { feature => feature.touch(document)}
			countTypes(document)
			countWords(document)
		}
	}
	def finishTraining(): Unit = {
		features.foreach(_.finishTraining())
	}
	def buildFeatureVector(vectorHandler: (Document, Array[Double]) => Unit): Unit = {
//		val allCases = features.map { feature => feature.extract().cases }
//
//		val allCombinations = allCases.foldLeft(Seq(Seq[Case]())) { (feature, cases) =>
//			cross(feature.toList, cases.toList)
//		}
		documents.foreach { post =>
			vectorHandler(post, features.map { feature => feature.extract().default(post) }.toArray.flatten)
		}
	}

	private def addFeature(feature: Feature): Unit = {
		features ::= feature
	}


//	def cross[X](x: Seq[X], y: Seq[X]): Seq[Seq[X]] = {
//		for (xi <- x; yi <- y) yield Vector(xi, yi)
//	}

	def cross[X](x: Seq[Seq[X]], y: Seq[X]): Seq[Seq[X]] = {
		for (xi <- x; yi <- y) yield xi :+ yi
	}

	private def countTypes(doc: Document): Unit = {
		genericCounter.classCounts(doc.documentClass) += 1
	}

	private def countWords(doc: Document): Unit = {
		doc.sentences.flatten.filter { word => !blacklist.exists(word.pos.startsWith)}.map(_.text).distinct.foreach { word =>
			if (!genericCounter.wordCounts.contains(word))
				genericCounter.wordCounts(word) = new mutable.HashMap[String, Int]().withDefaultValue(0)
			genericCounter.wordCounts(word)(doc.documentClass) += 1
		}
	}

	def removeClassCounts(className: String):Unit={
		genericCounter.classCounts.remove(className)
	}

	def takeTopOccurrence(className: String): List[(String, mutable.Map[String, Int], Double, Double)] = {
		genericCounter.takeTopOccurrence(className)
	}

	def takeTopOccurrence(className: String, threshold: Double): List[(String, mutable.Map[String, Int], Double, Double)] = {
		genericCounter.takeTopOccurrence(className, threshold)
	}

	def takeTopNotOccurrence(className: String): List[(String, mutable.Map[String, Int], Double, Double)] = {
		genericCounter.takeTopNotOccurrence(className)
	}

	def takeTopNotOccurrence(className: String, threshold: Double): List[(String, mutable.Map[String, Int], Double, Double)] = {
		genericCounter.takeTopNotOccurrence(className, threshold)
	}
}