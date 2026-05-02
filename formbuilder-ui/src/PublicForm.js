import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const API_BASE = "http://localhost:8080";

function normalizeOption(option) {
  if (typeof option === "string") return option;
  return option?.text || "";
}

function PublicForm() {
  const { token } = useParams();
  const [form, setForm] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [uploadingQuestionId, setUploadingQuestionId] = useState(null);
  const [respondentEmail, setRespondentEmail] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError("");

    fetch(`${API_BASE}/public/forms/${token}`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch form");
        return res.json();
      })
      .then((formData) => {
        setForm(formData);
        if (formData.timeLimitMinutes) {
          setSecondsLeft(Number(formData.timeLimitMinutes) * 60);
        }
        return fetch(`${API_BASE}/public/forms/${token}/questions`);
      })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch questions");
        return res.json();
      })
      .then((questionData) => setQuestions(Array.isArray(questionData) ? questionData : []))
      .catch((err) => setError(err.message || "Failed to fetch"))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    if (secondsLeft === null || submitted) return undefined;

    if (secondsLeft <= 0) {
      setError("Time is over. This form can no longer be submitted.");
      return undefined;
    }

    const timer = setTimeout(() => {
      setSecondsLeft((current) => (current === null ? null : current - 1));
    }, 1000);

    return () => clearTimeout(timer);
  }, [secondsLeft, submitted]);

  function formatSeconds(totalSeconds) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${String(seconds).padStart(2, "0")}`;
  }

  function updateAnswer(questionId, value) {
    setAnswers((currentAnswers) => ({
      ...currentAnswers,
      [questionId]: value,
    }));
  }

  function uploadFileAnswer(questionId, file) {
    if (!file) {
      updateAnswer(questionId, "");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    setUploadingQuestionId(questionId);
    setError("");

    fetch(`${API_BASE}/api/forms/upload`, {
      method: "POST",
      body: formData,
    })
      .then((res) => {
        if (!res.ok) throw new Error("File upload failed");
        return res.text();
      })
      .then((storedName) => updateAnswer(questionId, storedName))
      .catch((err) => {
        updateAnswer(questionId, "");
        setError(err.message || "File upload failed");
      })
      .finally(() => setUploadingQuestionId(null));
  }

  function isVisible(question) {
    if (!question.conditionQuestionId) return true;

    const parentAnswer = answers[question.conditionQuestionId];

    if (Array.isArray(parentAnswer)) {
      return parentAnswer.includes(question.conditionValue);
    }

    return parentAnswer === question.conditionValue;
  }

  function validate() {
    if (isClosed()) {
      setError("This form is closed.");
      return false;
    }

    if (secondsLeft !== null && secondsLeft <= 0) {
      setError("Time is over. This form can no longer be submitted.");
      return false;
    }

    if ((form?.collectEmail || form?.limitOneResponsePerEmail) && !respondentEmail.trim()) {
      setError("Email is required");
      return false;
    }

    const missingQuestion = questions
      .filter(isVisible)
      .find((question) => {
        if (!question.required) return false;

        const answer = answers[question.id];

        if (Array.isArray(answer)) return answer.length === 0;
        return !answer || String(answer).trim() === "";
      });

    if (missingQuestion) {
      setError(`${missingQuestion.text} is required`);
      return false;
    }

    return true;
  }

  function isClosed() {
    return form?.closeAt && new Date(form.closeAt) < new Date();
  }

  function submit() {
    if (!validate()) return;

    setSubmitting(true);
    setError("");

    fetch(`${API_BASE}/public/forms/${token}/submit`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        formId: form.id,
        respondentEmail,
        answers: Object.entries(answers)
          .filter(([questionId]) =>
            questions.some(
              (question) => question.id === Number(questionId) && isVisible(question)
            )
          )
          .map(([questionId, value]) => ({
            questionId: Number(questionId),
            answerText: Array.isArray(value) ? value.join(", ") : String(value || ""),
          })),
      }),
    })
      .then((res) => {
        if (!res.ok) {
          return res.text().then((message) => {
            throw new Error(message || "Failed to submit form");
          });
        }
        setSubmitted(true);
      })
      .catch((err) => setError(err.message || "Failed to submit"))
      .finally(() => setSubmitting(false));
  }

  function renderQuestion(question) {
    const options = (question.options || []).map(normalizeOption);

    return (
      <div className="question-card public-question" key={question.id}>
        <h3>
          {question.text}
          {question.required && " *"}
        </h3>

        {question.type === "TEXT" && (
          <input
            value={answers[question.id] || ""}
            onChange={(event) => updateAnswer(question.id, event.target.value)}
            placeholder="Your answer"
          />
        )}

        {question.type === "DATE" && (
          <input
            type="date"
            value={answers[question.id] || ""}
            onChange={(event) => updateAnswer(question.id, event.target.value)}
          />
        )}

        {question.type === "FILE" && (
          <>
            <input
              type="file"
              accept=".pdf,.doc,.docx,image/*"
              onChange={(event) => uploadFileAnswer(question.id, event.target.files?.[0])}
            />
            {uploadingQuestionId === question.id && (
              <p className="muted">Uploading file...</p>
            )}
            {answers[question.id] && uploadingQuestionId !== question.id && (
              <p className="muted">Uploaded: {answers[question.id]}</p>
            )}
          </>
        )}

        {question.type === "DROPDOWN" && (
          <select
            value={answers[question.id] || ""}
            onChange={(event) => updateAnswer(question.id, event.target.value)}
          >
            <option value="">Select an option</option>
            {options.map((option, index) => (
              <option key={`${question.id}-${index}`} value={option}>
                {option}
              </option>
            ))}
          </select>
        )}

        {question.type === "MCQ" && (
          <div className="choice-list">
            {options.map((option, index) => (
              <label key={`${question.id}-${index}`}>
                <input
                  type="radio"
                  name={`question-${question.id}`}
                  checked={answers[question.id] === option}
                  onChange={() => updateAnswer(question.id, option)}
                />
                {option}
              </label>
            ))}
          </div>
        )}

        {question.type === "CHECKBOX" && (
          <div className="choice-list">
            {options.map((option, index) => (
              <label key={`${question.id}-${index}`}>
                <input
                  type="checkbox"
                  checked={(answers[question.id] || []).includes(option)}
                  onChange={(event) => {
                    const previous = answers[question.id] || [];
                    const next = event.target.checked
                      ? [...previous, option]
                      : previous.filter((value) => value !== option);

                    updateAnswer(question.id, next);
                  }}
                />
                {option}
              </label>
            ))}
          </div>
        )}

        {question.type === "SECTION" && <div className="section-break">Section</div>}
      </div>
    );
  }

  if (loading) {
    return <div className="public-shell notice">Loading...</div>;
  }

  return (
    <div className="public-shell">
      {error && <div className="error-box">{error}</div>}

      {submitted ? (
        <div className="success-box">Submitted. Thank you.</div>
      ) : (
        <>
          <header className="public-header">
            <h1>{form?.title}</h1>
            {form?.description && <p>{form.description}</p>}
            {form?.closeAt && <p>Closes on {new Date(form.closeAt).toLocaleString()}</p>}
            {secondsLeft !== null && (
              <div className="notice">Time left: {formatSeconds(Math.max(secondsLeft, 0))}</div>
            )}
          </header>

          <div className="public-form">
            {(form?.collectEmail || form?.limitOneResponsePerEmail) && (
              <div className="question-card public-question">
                <h3>
                  Email address
                  {" *"}
                </h3>
                <input
                  type="email"
                  placeholder="yourname@example.com"
                  value={respondentEmail}
                  onChange={(event) => setRespondentEmail(event.target.value)}
                />
                {form?.limitOneResponsePerEmail && (
                  <p className="muted">Only one response is allowed for each email.</p>
                )}
              </div>
            )}

            {questions.filter(isVisible).map(renderQuestion)}

            <button
              disabled={
                submitting ||
                uploadingQuestionId !== null ||
                isClosed() ||
                (secondsLeft !== null && secondsLeft <= 0)
              }
              onClick={submit}
            >
              {submitting ? "Submitting..." : "Submit"}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default PublicForm;
