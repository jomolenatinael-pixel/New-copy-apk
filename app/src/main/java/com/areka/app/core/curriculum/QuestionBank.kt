package com.areka.app.core.curriculum

import com.areka.app.data.model.Flashcard
import com.areka.app.data.model.Question
import com.areka.app.data.model.Quiz
import com.areka.app.data.model.SubjectItem
import com.areka.app.data.model.SubjectUnit
import com.areka.app.data.repository.CurriculumData

/**
 * Clean curriculum and question bank interface.
 * Isolates UI and feature modules from static dataset structures and raw generated question files.
 */
interface QuestionBank {
    fun getSubjects(): List<SubjectItem>
    fun getSubject(subjectId: String): SubjectItem?
    fun getUnits(subjectId: String): List<SubjectUnit>
    fun getUnit(unitId: String): SubjectUnit?
    fun getQuizForUnit(unitId: String): Quiz
    fun getQuiz(quizId: String): Quiz?
    fun getAllQuizzes(): List<Quiz>
    fun getFlashcards(unitId: String): List<Flashcard>
    fun getAllFlashcards(): List<Flashcard>
}

class DefaultQuestionBank : QuestionBank {
    override fun getSubjects(): List<SubjectItem> = CurriculumData.subjects

    override fun getSubject(subjectId: String): SubjectItem? =
        CurriculumData.subjects.find { it.id.equals(subjectId, ignoreCase = true) }

    override fun getUnits(subjectId: String): List<SubjectUnit> =
        CurriculumData.getUnitsForSubject(subjectId)

    override fun getUnit(unitId: String): SubjectUnit? =
        CurriculumData.units.find { it.id.equals(unitId, ignoreCase = true) }

    override fun getQuizForUnit(unitId: String): Quiz =
        CurriculumData.getQuizForUnit(unitId)

    override fun getQuiz(quizId: String): Quiz? {
        // Try finding by quiz id or fallback to unit id
        val direct = CurriculumData.getAllCurriculumQuizzes().find { it.id == quizId }
        if (direct != null) return direct
        val unit = CurriculumData.units.find { "quiz_${it.id}" == quizId || it.id == quizId }
        return unit?.let { getQuizForUnit(it.id) }
    }

    override fun getAllQuizzes(): List<Quiz> =
        CurriculumData.getAllCurriculumQuizzes()

    override fun getFlashcards(unitId: String): List<Flashcard> =
        CurriculumData.getFlashcardsForUnit(unitId)

    override fun getAllFlashcards(): List<Flashcard> =
        CurriculumData.flashcards
}
