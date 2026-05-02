import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Link,
  Navigate,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";
import { DndContext } from "@dnd-kit/core";
import {
  SortableContext,
  arrayMove,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import PublicForm from "./PublicForm";

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

const API_BASE = "http://localhost:8080/api/forms";
const ROOT_BASE = "http://localhost:8080";
const AUTH_STORAGE_KEY = "formix-auth-token";
const OPTION_QUESTION_TYPES = ["MCQ", "CHECKBOX", "DROPDOWN"];
const TEMPLATE_OPTIONS = [
  { type: "feedback", name: "Customer Feedback", detail: "Satisfaction and improvement ideas" },
  { type: "job_application", name: "Job Application", detail: "Candidate details and resume" },
  { type: "event_registration", name: "Event Registration", detail: "Attendees, tickets, and meals" },
  { type: "contact", name: "Contact Form", detail: "Messages and enquiries" },
  { type: "student_admission", name: "Student Admission", detail: "Student and guardian info" },
  { type: "appointment", name: "Appointment Request", detail: "Meeting date and purpose" },
  { type: "support_ticket", name: "Support Ticket", detail: "Issue category and priority" },
  { type: "product_order", name: "Product Order", detail: "Order and delivery details" },
  { type: "employee_onboarding", name: "Employee Onboarding", detail: "Joining and documents" },
  { type: "leave_request", name: "Leave Request", detail: "Dates, type, and reason" },
  { type: "customer_survey", name: "Customer Survey", detail: "Experience and preferences" },
  { type: "newsletter", name: "Newsletter Signup", detail: "Email and topic interests" },
  { type: "volunteer", name: "Volunteer Registration", detail: "Availability and interests" },
  { type: "rental_application", name: "Rental Application", detail: "Tenant details and documents" },
  { type: "project_request", name: "Project Request", detail: "Requirements and budget" },
];
const DEFAULT_ANALYTICS = {
  totalResponses: 0,
  questions: [],
};

function isOptionQuestionType(type) {
  return OPTION_QUESTION_TYPES.includes(type);
}

function normalizeOption(option) {
  if (typeof option === "string") {
    return {
      id: null,
      text: option,
    };
  }

  return {
    id: option?.id || null,
    text: option?.text || "",
  };
}

function getOptionText(option) {
  return typeof option === "string" ? option : option?.text || "";
}

function normalizeQuestion(question) {
  return {
    ...question,
    options: (question.options || []).map(normalizeOption),
    required: Boolean(question.required),
  };
}

function getQuestionPayload(question) {
  const { options, ...payload } = question;
  return payload;
}

function toDateTimeLocalValue(value) {
  if (!value) return "";
  return value.slice(0, 16);
}

function SortableQuestion({ id, disabled, children }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
  } = useSortable({ id, disabled });

  return (
    <div
      ref={setNodeRef}
      className="question-card"
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
      }}
    >
      {children({ attributes, listeners })}
    </div>
  );
}

function AnalyticsChart({ question }) {
  const counts = question.answerCount || {};
  const labels = Object.keys(counts);
  const palette = ["#2563eb", "#f97316", "#14b8a6", "#e11d48", "#7c3aed", "#facc15"];

  if (labels.length === 0) {
    return <p className="muted">No answers yet.</p>;
  }

  return (
    <Bar
      data={{
        labels,
        datasets: [
          {
            label: "Responses",
            data: labels.map((label) => counts[label]),
            backgroundColor: labels.map((_, index) => palette[index % palette.length]),
            borderRadius: 12,
            borderSkipped: false,
          },
        ],
      }}
      options={{
        responsive: true,
        plugins: {
          legend: {
            display: false,
          },
        },
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: {
              display: false,
            },
          },
          y: {
            beginAtZero: true,
            grid: {
              color: "rgba(15, 23, 42, 0.08)",
            },
            ticks: {
              precision: 0,
            },
          },
        },
      }}
    />
  );
}

function SimpleBarChart({ labels = [], values = [], label = "Count" }) {
  const palette = ["#14b8a6", "#2563eb", "#f97316", "#e11d48", "#7c3aed", "#22c55e"];

  if (!labels.length) {
    return <p className="muted">No chart data yet.</p>;
  }

  return (
    <Bar
      data={{
        labels,
        datasets: [
          {
            label,
            data: values,
            backgroundColor: labels.map((_, index) => palette[index % palette.length]),
            borderRadius: 12,
            borderSkipped: false,
          },
        ],
      }}
      options={{
        responsive: true,
        plugins: {
          legend: {
            display: false,
          },
        },
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: {
              display: false,
            },
          },
          y: {
            beginAtZero: true,
            grid: {
              color: "rgba(15, 23, 42, 0.08)",
            },
            ticks: {
              precision: 0,
            },
          },
        },
      }}
    />
  );
}

function LoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [authForm, setAuthForm] = useState({ username: "", password: "" });
  const [authMode, setAuthMode] = useState("login");
  const [authStatus, setAuthStatus] = useState("");
  const [authLoading, setAuthLoading] = useState(false);

  function submitAuth(action, event) {
    event?.preventDefault();
    setAuthStatus(
      action === "login"
        ? "Signing you in..."
        : action === "register"
          ? "Creating your account..."
          : "Updating your password..."
    );
    setAuthLoading(true);

    fetch(`${ROOT_BASE}/auth/${action}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(authForm),
    })
      .then((res) =>
        res.text().then((message) => {
          if (!res.ok) throw new Error(message || `${action} failed`);
          return message;
        })
      )
      .then((message) => {
        if (action === "login") {
          localStorage.setItem(AUTH_STORAGE_KEY, message);
          onLogin(message);
          navigate("/home");
          return;
        }

        setAuthStatus(
          action === "register"
            ? "Account created. Use Login to enter your workspace."
            : message
        );
        setAuthMode("login");
      })
      .catch((err) => setAuthStatus(err.message || `${action} failed`))
      .finally(() => setAuthLoading(false));
  }

  return (
    <main className="login-page">
      <section className="login-hero">
        <span className="eyebrow">Formix Studio</span>
        <h1>Build private forms. Read sharper answers.</h1>
        <p>
          Sign in first, then manage forms, publish links, and review AI-assisted response
          summaries from a focused professional dashboard.
        </p>
        <div className="login-highlights">
          <span>Live form builder</span>
          <span>AI summaries</span>
          <span>Analytics dashboard</span>
        </div>
      </section>

      <section className="login-card">
        <span className="eyebrow">Welcome Back</span>
        <h2>{authMode === "forgot" ? "Reset password" : "Login to continue"}</h2>
        <p className="muted">
          {authMode === "forgot"
            ? "Enter your username and choose a new password."
            : "Use your Formix account credentials."}
        </p>

        <form
          className="stack"
          onSubmit={(event) =>
            submitAuth(authMode === "forgot" ? "forgot-password" : "login", event)
          }
        >
          <input
            placeholder="Username"
            value={authForm.username}
            autoComplete="username"
            onChange={(event) =>
              setAuthForm((current) => ({
                ...current,
                username: event.target.value,
              }))
            }
          />
          <input
            placeholder={authMode === "forgot" ? "New password" : "Password"}
            type="password"
            value={authForm.password}
            autoComplete={authMode === "forgot" ? "new-password" : "current-password"}
            onChange={(event) =>
              setAuthForm((current) => ({
                ...current,
                password: event.target.value,
              }))
            }
          />
          <button type="submit" disabled={authLoading}>
            {authLoading ? "Please wait..." : authMode === "forgot" ? "Update Password" : "Login"}
          </button>
          {authMode === "login" ? (
            <>
              <button
                type="button"
                className="soft-button"
                disabled={authLoading}
                onClick={() => submitAuth("register")}
              >
                Create Account
              </button>
              <button
                type="button"
                className="text-button"
                disabled={authLoading}
                onClick={() => {
                  setAuthMode("forgot");
                  setAuthStatus("");
                }}
              >
                Forgot password?
              </button>
            </>
          ) : (
            <button
              type="button"
              className="text-button"
              disabled={authLoading}
              onClick={() => {
                setAuthMode("login");
                setAuthStatus("");
              }}
            >
              Back to login
            </button>
          )}
          {authStatus && <div className="auth-status">{authStatus}</div>}
        </form>
      </section>
    </main>
  );
}

function Builder({ authToken, onLogout }) {
  const navigate = useNavigate();
  const { formId } = useParams();
  const isEditor = Boolean(formId);
  const [forms, setForms] = useState([]);
  const [selectedForm, setSelectedForm] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [previewMode, setPreviewMode] = useState(false);
  const [tab, setTab] = useState("questions");
  const [analytics, setAnalytics] = useState(DEFAULT_ANALYTICS);
  const [answers, setAnswers] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [formSearch, setFormSearch] = useState("");
  const [aiPrompt, setAiPrompt] = useState("");
  const [aiOutput, setAiOutput] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const [insights, setInsights] = useState([]);
  const [aiSummary, setAiSummary] = useState("");
  const [chartData, setChartData] = useState({ labels: [], values: [] });

  function apiFetch(url, options = {}) {
    return fetch(url, {
      ...options,
      headers: {
        ...(options.headers || {}),
        Authorization: `Bearer ${authToken}`,
      },
    });
  }

  useEffect(() => {
    fetchForms();
  }, [authToken]);

  useEffect(() => {
    if (!formId) {
      setSelectedForm(null);
      setQuestions([]);
      setAnswers({});
      return;
    }

    const form = forms.find((candidate) => String(candidate.id) === formId);

    if (form && selectedForm?.id !== form.id) {
      setSelectedForm(form);
      loadQuestions(form);
    }
  }, [formId, forms, selectedForm?.id]);

  function fetchForms() {
    setLoading(true);
    setError("");

    apiFetch(API_BASE)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch forms");
        return res.json();
      })
      .then((data) => setForms(Array.isArray(data) ? data : []))
      .catch((err) => setError(err.message || "Failed to fetch forms"))
      .finally(() => setLoading(false));
  }

  function updateQuestion(index, changes, shouldSave = true) {
    const updated = questions.map((question, questionIndex) =>
      questionIndex === index ? { ...question, ...changes } : question
    );

    setQuestions(updated);

    if (shouldSave) {
      saveQuestion(updated[index]);
    }
  }

  function loadQuestions(form) {
    setError("");

    apiFetch(`${API_BASE}/${form.id}/questions`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch questions");
        return res.json();
      })
      .then((data) => {
        const enriched = Array.isArray(data) ? data.map(normalizeQuestion) : [];
        setQuestions(enriched);
        setAnswers({});
        setTab("questions");
      })
      .catch((err) => setError(err.message || "Failed to fetch questions"));
  }

  function saveFormDetails(changes) {
    if (!selectedForm) return;

    const updatedForm = {
      ...selectedForm,
      ...changes,
    };

    setSelectedForm(updatedForm);
    setForms((currentForms) =>
      currentForms.map((form) => (form.id === updatedForm.id ? updatedForm : form))
    );

    apiFetch(`${API_BASE}/${updatedForm.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(updatedForm),
    }).catch(console.error);
  }

  function saveQuestion(question) {
    if (!question?.id || String(question.id).startsWith("local-")) return;

    apiFetch(`${API_BASE}/question/${question.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(getQuestionPayload(question)),
    }).catch(console.error);
  }

  function saveQuestionOrder(updated) {
    updated.forEach((question, index) => {
      if (!question.id || String(question.id).startsWith("local-")) return;

      apiFetch(`${API_BASE}/question/${question.id}/order`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          displayOrder: index,
        }),
      }).catch(console.error);
    });
  }

  function loadAnalytics() {
    if (!selectedForm) return;

    setError("");

    Promise.all([
      apiFetch(`${API_BASE}/analytics/${selectedForm.id}`).then((res) => {
        if (!res.ok) throw new Error("Failed to fetch analytics");
        return res.json();
      }),
      apiFetch(`${API_BASE}/insights/${selectedForm.id}`).then((res) => {
        if (!res.ok) throw new Error("Failed to fetch insights");
        return res.json();
      }),
      apiFetch(`${API_BASE}/chart/${selectedForm.id}`).then((res) => {
        if (!res.ok) throw new Error("Failed to fetch chart");
        return res.json();
      }),
    ])
      .then(([analyticsData, insightData, chart]) => {
        setAnalytics(analyticsData || DEFAULT_ANALYTICS);
        setInsights(insightData?.insights || []);
        setChartData(chart || { labels: [], values: [] });
      })
      .catch((err) => setError(err.message || "Failed to fetch analytics"));
  }

  function loadAiSummary() {
    if (!selectedForm) return;

    setAiSummary("Loading AI summary...");

    apiFetch(`${API_BASE}/ai-summary/${selectedForm.id}`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch AI summary");
        return res.text();
      })
      .then((text) => setAiSummary(text))
      .catch((err) => setAiSummary(err.message || "AI summary failed"));
  }

  function publishForm() {
    if (!selectedForm?.shareToken) return;

    const url = `http://localhost:3000/form/${selectedForm.shareToken}`;

    navigator.clipboard
      .writeText(url)
      .then(() => alert("Public URL copied"))
      .catch(() => alert(url));
  }

  function exportResponsesCsv() {
    if (!selectedForm) return;

    apiFetch(`${API_BASE}/export/${selectedForm.id}`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to export CSV");
        return res.text();
      })
      .then((csv) => {
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = url;
        link.download = `${selectedForm.title || "form"}-responses.csv`;
        link.click();
        URL.revokeObjectURL(url);
      })
      .catch((err) => setError(err.message || "Failed to export CSV"));
  }

  function generatePromptDraft() {
    const topic = aiPrompt.trim();
    if (!topic) {
      setAiOutput("Write a prompt first, like: customer feedback for a cafe.");
      return;
    }

    setAiLoading(true);
    setAiOutput("");

    apiFetch(`${API_BASE}/ai-questions?topic=${encodeURIComponent(topic)}`)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to generate questions");
        return res.text();
      })
      .then((text) => setAiOutput(text))
      .catch((err) => setAiOutput(err.message || "AI generation is not available yet"))
      .finally(() => setAiLoading(false));
  }

  function deleteForm(formId) {
    const form = forms.find((candidate) => candidate.id === formId);
    if (!form) return;

    if (!window.confirm(`Delete "${form.title}"?`)) return;

    apiFetch(`${API_BASE}/${formId}`, {
      method: "DELETE",
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to delete form");

        setForms((currentForms) =>
          currentForms.filter((currentForm) => currentForm.id !== formId)
        );

        if (selectedForm?.id === formId) {
          setSelectedForm(null);
          setQuestions([]);
          setAnswers({});
          navigate("/home");
        }
      })
      .catch((err) => setError(err.message || "Failed to delete form"));
  }

  function cloneForm(formId) {
    apiFetch(`${API_BASE}/${formId}/clone`, {
      method: "POST",
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to clone form");
        return res.json();
      })
      .then((clonedForm) => {
        setForms((currentForms) => [...currentForms, clonedForm]);
        navigate(`/builder/${clonedForm.id}`);
      })
      .catch((err) => setError(err.message || "Failed to clone form"));
  }

  function createTemplate(type = "feedback") {
    apiFetch(`${API_BASE}/template?type=${encodeURIComponent(type)}`, {
      method: "POST",
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to create template");
        return res.json();
      })
      .then((templateForm) => {
        if (!templateForm?.id) {
          throw new Error("Template was not created. Restart the backend and try again.");
        }
        setForms((currentForms) => [...currentForms, templateForm]);
        navigate(`/builder/${templateForm.id}`);
      })
      .catch((err) => setError(err.message || "Failed to create template"));
  }

  function createForm() {
    setError("");

    apiFetch(API_BASE, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        title: "New Form",
        description: "Draft form",
      }),
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to create form");
        return res.json();
      })
      .then((newForm) => {
        setForms((currentForms) => [...currentForms, newForm]);
        setSelectedForm(newForm);
        setQuestions([]);
        setAnswers({});
        navigate(`/builder/${newForm.id}`);
      })
      .catch((err) => setError(err.message || "Failed to create form"));
  }

  function addQuestion(type = "TEXT") {
    if (!selectedForm) return;

    apiFetch(`${API_BASE}/question`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        text: "New Question",
        type,
        required: false,
        form: {
          id: selectedForm.id,
        },
      }),
    })
      .then((res) => {
        if (!res.ok) {
          return res.text().then((message) => {
            throw new Error(message || "Failed to add question");
          });
        }
        return res.json();
      })
      .then((newQuestion) => {
        const updated = [...questions, normalizeQuestion(newQuestion)];
        setQuestions(updated);
        saveQuestionOrder(updated);
      })
      .catch((err) => setError(err.message || "Failed to add question"));
  }

  function deleteQuestion(index) {
    const question = questions[index];
    if (!question) return;

    if (question.id && !String(question.id).startsWith("local-")) {
      apiFetch(`${API_BASE}/question/${question.id}`, {
        method: "DELETE",
      }).catch(console.error);
    }

    const updated = questions.filter((_, questionIndex) => questionIndex !== index);
    setQuestions(updated);
    saveQuestionOrder(updated);
  }

  function duplicateQuestion(index) {
    const question = questions[index];
    if (!question || !selectedForm) return;

    apiFetch(`${API_BASE}/question`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        ...getQuestionPayload(question),
        id: null,
        form: { id: selectedForm.id },
      }),
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to duplicate question");
        return res.json();
      })
      .then((newQuestion) => {
        const updated = [
          ...questions.slice(0, index + 1),
          normalizeQuestion({
            ...newQuestion,
            options: question.options,
          }),
          ...questions.slice(index + 1),
        ];

        setQuestions(updated);
        saveQuestionOrder(updated);
      })
      .catch((err) => setError(err.message || "Failed to duplicate question"));
  }

  function moveQuestion(index, direction) {
    const newIndex = index + direction;

    if (newIndex < 0 || newIndex >= questions.length) return;

    const updated = arrayMove(questions, index, newIndex);
    setQuestions(updated);
    saveQuestionOrder(updated);
  }

  function handleDragEnd(event) {
    const { active, over } = event;

    if (!over || active.id === over.id) return;

    const oldIndex = questions.findIndex((question) => question.id === active.id);
    const newIndex = questions.findIndex((question) => question.id === over.id);

    if (oldIndex === -1 || newIndex === -1) return;

    const reordered = arrayMove(questions, oldIndex, newIndex);

    setQuestions(reordered);
    saveQuestionOrder(reordered);
  }

  function addOption(questionIndex, text = "New Option") {
    const question = questions[questionIndex];
    if (!question) return;

    const pendingOption = {
      id: null,
      text,
    };

    const applyOption = (option) => {
      setQuestions((currentQuestions) =>
        currentQuestions.map((currentQuestion, index) =>
          index === questionIndex
            ? {
                ...currentQuestion,
                options: [...(currentQuestion.options || []), normalizeOption(option)],
              }
            : currentQuestion
        )
      );
    };

    if (!question.id || String(question.id).startsWith("local-")) {
      applyOption(pendingOption);
      return;
    }

    apiFetch(`${API_BASE}/questions/${question.id}/option?text=${encodeURIComponent(text)}`, {
      method: "POST",
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to add option");
        return res.json();
      })
      .then(applyOption)
      .catch((err) => setError(err.message || "Failed to add option"));
  }

  function handleQuestionTypeChange(index, type) {
    const question = questions[index];

    updateQuestion(index, { type });

    if (isOptionQuestionType(type) && !(question.options || []).length) {
      addOption(index, "Option 1");
      addOption(index, "Option 2");
    }
  }

  function updateOption(questionIndex, optionIndex, value) {
    const updated = questions.map((question, index) => {
      if (index !== questionIndex) {
        return question;
      }

      const options = [...(question.options || [])];
      options[optionIndex] = {
        ...normalizeOption(options[optionIndex]),
        text: value,
      };

      return {
        ...question,
        options,
      };
    });

    setQuestions(updated);

    const option = updated[questionIndex]?.options?.[optionIndex];
    if (option?.id) {
      apiFetch(`${API_BASE}/options/${option.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(option),
      }).catch(console.error);
    }

    saveQuestion(updated[questionIndex]);
  }

  function updateAnswer(questionId, value) {
    setAnswers((currentAnswers) => ({
      ...currentAnswers,
      [questionId]: value,
    }));
  }

  function getParentQuestion(question) {
    return questions.find((candidate) => candidate.id === question.conditionQuestionId);
  }

  function renderConditionValueControl(question, index) {
    const parentQuestion = getParentQuestion(question);
    const parentOptions = parentQuestion?.options || [];

    if (isOptionQuestionType(parentQuestion?.type) && parentOptions.length) {
      return (
        <select
          value={question.conditionValue || ""}
          onChange={(event) =>
            updateQuestion(index, {
              conditionValue: event.target.value,
            })
          }
        >
          <option value="">Select answer</option>
          {parentOptions.map((option, optionIndex) => {
            const text = getOptionText(option);
            return (
              <option key={`${parentQuestion.id}-condition-${optionIndex}`} value={text}>
                {text}
              </option>
            );
          })}
        </select>
      );
    }

    return (
      <input
        placeholder="Show when answer equals..."
        value={question.conditionValue || ""}
        onChange={(event) =>
          updateQuestion(index, {
            conditionValue: event.target.value,
          })
        }
      />
    );
  }

  function renderPreviewQuestion(question) {
    return (
      <div>
        <h3>
          {question.text}
          {question.required && " *"}
        </h3>

        {question.type === "TEXT" && (
          <input
            placeholder="Your answer"
            value={answers[question.id] || ""}
            onChange={(event) => updateAnswer(question.id, event.target.value)}
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
          <input
            type="file"
            onChange={(event) => updateAnswer(question.id, event.target.files?.[0]?.name || "")}
          />
        )}

        {question.type === "DROPDOWN" && (
          <select
            value={answers[question.id] || ""}
            onChange={(event) => updateAnswer(question.id, event.target.value)}
          >
            <option value="">Select an option</option>
            {(question.options || []).map((option, optionIndex) => {
              const text = getOptionText(option);
              return (
                <option key={`${question.id}-preview-option-${optionIndex}`} value={text}>
                  {text}
                </option>
              );
            })}
          </select>
        )}

        {(question.type === "MCQ" || question.type === "CHECKBOX") && (
          <div className="choice-list">
            {(question.options || []).map((option, optionIndex) => {
              const text = getOptionText(option);

              return (
                <label key={`${question.id}-preview-option-${optionIndex}`}>
                  <input
                    type={question.type === "MCQ" ? "radio" : "checkbox"}
                    name={`question-${question.id}`}
                    checked={
                      question.type === "MCQ"
                        ? answers[question.id] === text
                        : (answers[question.id] || []).includes(text)
                    }
                    onChange={(event) => {
                      if (question.type === "MCQ") {
                        updateAnswer(question.id, text);
                        return;
                      }

                      const previousAnswers = answers[question.id] || [];
                      const nextAnswers = event.target.checked
                        ? [...previousAnswers, text]
                        : previousAnswers.filter((value) => value !== text);

                      updateAnswer(question.id, nextAnswers);
                    }}
                  />
                  {text}
                </label>
              );
            })}
          </div>
        )}

        {question.type === "SECTION" && <div className="section-break">New Page Section</div>}
      </div>
    );
  }

  function shouldShowQuestion(question) {
    if (!question.conditionQuestionId) return true;

    const parentAnswer = answers[question.conditionQuestionId];

    if (Array.isArray(parentAnswer)) {
      return parentAnswer.includes(question.conditionValue);
    }

    return parentAnswer === question.conditionValue;
  }

  const visibleForms = forms.filter((form) => {
    const search = formSearch.trim().toLowerCase();
    if (!search) return true;

    return [form.title, form.description, form.shareToken]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(search));
  });

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">{isEditor ? "Formix Workspace" : "Command Center"}</span>
          <h1>{isEditor ? selectedForm?.title || "Form Editor" : "Formix"}</h1>
          <p>
            {isEditor
              ? "Create questions, preview the form, and share it."
              : "Create, publish, and understand private forms from one professional workspace."}
          </p>
        </div>
        <div className="actions">
          {isEditor ? (
            <>
              <Link className="button-link secondary" to="/home">
                Home
              </Link>
              <button onClick={() => setPreviewMode((current) => !current)}>
                {previewMode ? "Edit" : "Preview"}
              </button>
              <button onClick={publishForm}>Share Link</button>
            </>
          ) : (
            <button onClick={createForm}>Create Form</button>
          )}
          <button className="ghost-button" onClick={onLogout}>
            Sign Out
          </button>
        </div>
      </header>

      {loading && <div className="notice">Loading...</div>}
      {error && <div className="error-box">{error}</div>}

      {!isEditor && (
        <section className="home-summary">
          <div className="summary-card coral">
            <span>Total Forms</span>
            <strong>{forms.length}</strong>
          </div>
          <div className="summary-card blue">
            <span>Published Links</span>
            <strong>{forms.filter((form) => form.shareToken).length}</strong>
          </div>
          <div className="summary-card green">
            <span>Workspace</span>
            <strong>Ready</strong>
          </div>
        </section>
      )}

      <main className={isEditor ? "editor-layout" : "dashboard-layout"}>
        {!isEditor && (
          <div className="dashboard-grid">
            <section className="ai-panel">
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">AI Assist</span>
                  <h2>Generate Form With Prompt</h2>
                  <p>Draft smarter questions for surveys, feedback, and registrations.</p>
                </div>
              </div>

              <textarea
                placeholder="Example: Create a customer feedback form for a bakery"
                value={aiPrompt}
                onChange={(event) => setAiPrompt(event.target.value)}
              />

              <div className="ai-actions">
                <button onClick={generatePromptDraft} disabled={aiLoading}>
                  {aiLoading ? "Generating..." : "Generate Draft"}
                </button>
                <button
                  className="soft-button"
                  onClick={() => {
                    setAiPrompt("");
                    setAiOutput("");
                  }}
                >
                  Clear
                </button>
              </div>

              {aiOutput && <pre className="ai-output">{aiOutput}</pre>}
            </section>

            <section className="utility-panel">
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">Quick Start</span>
                  <h2>Built-in Templates</h2>
                  <p>Pick a ready form, then edit the title, questions, options, and required fields.</p>
                </div>
              </div>

              <div className="template-grid">
                {TEMPLATE_OPTIONS.map((template) => (
                  <button
                    className="template-button"
                    key={template.type}
                    onClick={() => createTemplate(template.type)}
                  >
                    <strong>{template.name}</strong>
                    <span>{template.detail}</span>
                  </button>
                ))}
              </div>
            </section>

            <section className="forms-panel">
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">Library</span>
                  <h2>All Created Forms</h2>
                  <p>Open, share, or clean up your saved forms.</p>
                </div>
                <span>{visibleForms.length}</span>
              </div>

              <input
                className="search-input"
                placeholder="Search forms"
                value={formSearch}
                onChange={(event) => setFormSearch(event.target.value)}
              />

              <div className="forms-grid">
                {visibleForms.map((form) => (
                  <div
                    className={`form-card ${selectedForm?.id === form.id ? "active" : ""}`}
                    key={form.id}
                  >
                    <button
                      className="form-select"
                      onClick={() => navigate(`/builder/${form.id}`)}
                    >
                      <strong>{form.title || "Untitled Form"}</strong>
                      <span>{form.description || "No description"}</span>
                      {form.shareToken && <small>/form/{form.shareToken}</small>}
                    </button>
                    <div className="form-card-actions">
                      <button onClick={() => navigate(`/builder/${form.id}`)}>Open</button>
                      <button className="soft-button" onClick={() => cloneForm(form.id)}>
                        Clone
                      </button>
                      <button
                        className="danger-button"
                        onClick={() => deleteForm(form.id)}
                        title="Delete form"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {!loading && visibleForms.length === 0 && (
                <p className="muted">No forms match your search.</p>
              )}
            </section>
          </div>
        )}

        {selectedForm ? (
          <>
            <section className="workspace">
              <div className="form-title-panel">
                <span className="eyebrow">Form Details</span>
                <label>
                  <span>Form title</span>
                  <input
                    className="title-input"
                    placeholder="Example: Job Application Form"
                    value={selectedForm.title || ""}
                    onChange={(event) => saveFormDetails({ title: event.target.value })}
                  />
                </label>
                <label>
                  <span>Description</span>
                  <textarea
                    className="description-input"
                    placeholder="Add a short message for people filling this form."
                    value={selectedForm.description || ""}
                    onChange={(event) =>
                      saveFormDetails({ description: event.target.value })
                    }
                  />
                </label>

                <div className="form-settings-grid">
                  <label className="checkbox-label setting-toggle">
                    <input
                      type="checkbox"
                      checked={Boolean(selectedForm.collectEmail)}
                      onChange={(event) =>
                        saveFormDetails({ collectEmail: event.target.checked })
                      }
                    />
                    Collect respondent email
                  </label>
                  <label className="checkbox-label setting-toggle">
                    <input
                      type="checkbox"
                      checked={Boolean(selectedForm.limitOneResponsePerEmail)}
                      onChange={(event) =>
                        saveFormDetails({
                          limitOneResponsePerEmail: event.target.checked,
                          collectEmail: event.target.checked ? true : selectedForm.collectEmail,
                        })
                      }
                    />
                    Limit to one response per email
                  </label>
                  <label>
                    <span>Close form at</span>
                    <input
                      type="datetime-local"
                      value={toDateTimeLocalValue(selectedForm.closeAt)}
                      onChange={(event) =>
                        saveFormDetails({ closeAt: event.target.value || null })
                      }
                    />
                  </label>
                  <label>
                    <span>Timer limit in minutes</span>
                    <input
                      type="number"
                      min="0"
                      placeholder="No timer"
                      value={selectedForm.timeLimitMinutes || ""}
                      onChange={(event) =>
                        saveFormDetails({
                          timeLimitMinutes: event.target.value
                            ? Number(event.target.value)
                            : null,
                        })
                      }
                    />
                  </label>
                </div>
              </div>

              <div className="tabs">
                <button
                  className={tab === "questions" ? "active" : ""}
                  onClick={() => setTab("questions")}
                >
                  Questions
                </button>
                <button
                  className={tab === "responses" ? "active" : ""}
                  onClick={() => {
                    setTab("responses");
                    loadAnalytics();
                  }}
                >
                  Responses
                </button>
                {tab === "responses" && (
                  <button onClick={exportResponsesCsv}>Export CSV</button>
                )}
                {tab === "questions" && <button onClick={() => addQuestion()}>Add Question</button>}
              </div>

              {tab === "responses" && (
                <div className="analytics-grid">
                  <div className="analytics-hero">
                    <div>
                      <span className="eyebrow">Analytical Dashboard</span>
                      <h2>Response intelligence for {selectedForm.title}</h2>
                      <p>
                        Track submissions, compare answer patterns, and generate a quick AI
                        summary for decision-ready review.
                      </p>
                    </div>
                    <button onClick={loadAiSummary}>Generate AI Summary</button>
                  </div>

                  <div className="metric-card accent-orange">
                    <span>Total Responses</span>
                    <strong>{analytics.totalResponses}</strong>
                    <p>Collected submissions across this form.</p>
                  </div>

                  <div className="metric-card accent-teal">
                    <span>Questions Tracked</span>
                    <strong>{(analytics.questions || []).length}</strong>
                    <p>Analytics-ready question blocks.</p>
                  </div>

                  <div className="chart-card">
                    <h3>Overall Answer Chart</h3>
                    <div className="chart-frame">
                      <SimpleBarChart
                        labels={chartData.labels || []}
                        values={chartData.values || []}
                        label="Answers"
                      />
                    </div>
                  </div>

                  <div className="chart-card ai-summary-card">
                    <div className="panel-heading">
                      <div>
                        <h3>AI Summary</h3>
                        <p>Let the assistant explain response trends in plain language.</p>
                      </div>
                      <button className="soft-button" onClick={loadAiSummary}>
                        Refresh
                      </button>
                    </div>
                    {aiSummary ? (
                      <pre className="ai-output">{aiSummary}</pre>
                    ) : (
                      <p className="muted">
                        Click Generate AI Summary to create a concise analytical overview.
                      </p>
                    )}
                  </div>

                  <div className="chart-card insight-card">
                    <h3>Insights</h3>
                    {insights.length ? (
                      <ul className="insight-list">
                        {insights.map((insight, index) => (
                          <li key={`${insight}-${index}`}>{insight}</li>
                        ))}
                      </ul>
                    ) : (
                      <p className="muted">No insights yet.</p>
                    )}
                  </div>

                  {(analytics.questions || []).map((question, index) => (
                    <div className="chart-card" key={`${question.questionText}-${index}`}>
                      <h3>{question.questionText}</h3>
                      <div className="chart-frame small">
                        <AnalyticsChart question={question} />
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {tab === "questions" && (
                <DndContext onDragEnd={handleDragEnd}>
                  <SortableContext
                    items={questions.map((question) => question.id)}
                    strategy={verticalListSortingStrategy}
                  >
                    {questions.map((question, index) => {
                      if (previewMode && !shouldShowQuestion(question)) return null;

                      return (
                        <SortableQuestion
                          key={question.id}
                          id={question.id}
                          disabled={previewMode}
                        >
                          {({ attributes, listeners }) => (
                            <>
                              {!previewMode && (
                                <>
                                  <div
                                    className="drag-handle"
                                    {...attributes}
                                    {...listeners}
                                  >
                                    Drag Handle
                                  </div>

                                  <input
                                    value={question.text}
                                    onChange={(event) =>
                                      updateQuestion(index, { text: event.target.value })
                                    }
                                  />

                                  <div className="row">
                                    <select
                                      value={question.type}
                                      onChange={(event) =>
                                        handleQuestionTypeChange(index, event.target.value)
                                      }
                                    >
                                      <option value="TEXT">Short Answer</option>
                                      <option value="DATE">Date / DOB</option>
                                      <option value="MCQ">Multiple Choice</option>
                                      <option value="CHECKBOX">Checkbox</option>
                                      <option value="DROPDOWN">Dropdown</option>
                                      <option value="FILE">File Upload / Resume</option>
                                      <option value="SECTION">Section Break</option>
                                    </select>

                                    <label className="checkbox-label">
                                      <input
                                        type="checkbox"
                                        checked={question.required || false}
                                        onChange={(event) =>
                                          updateQuestion(index, {
                                            required: event.target.checked,
                                          })
                                        }
                                      />
                                      Required
                                    </label>
                                  </div>

                                  <div className="condition-panel">
                                    <label>Show only when</label>
                                    <select
                                      value={question.conditionQuestionId || ""}
                                      onChange={(event) =>
                                        updateQuestion(index, {
                                          conditionQuestionId: event.target.value
                                            ? Number(event.target.value)
                                            : null,
                                          conditionValue: "",
                                        })
                                      }
                                    >
                                      <option value="">Always show</option>
                                      {questions
                                        .filter((candidate) => candidate.id !== question.id)
                                        .map((candidate) => (
                                          <option key={candidate.id} value={candidate.id}>
                                            {candidate.text}
                                          </option>
                                        ))}
                                    </select>
                                    {question.conditionQuestionId &&
                                      renderConditionValueControl(question, index)}
                                  </div>

                                  <div className="question-actions">
                                    <button onClick={() => addQuestion()}>Add Question</button>
                                    <button onClick={() => duplicateQuestion(index)}>
                                      Duplicate
                                    </button>
                                    <button onClick={() => deleteQuestion(index)}>Delete</button>
                                    <button onClick={() => moveQuestion(index, -1)}>Up</button>
                                    <button onClick={() => moveQuestion(index, 1)}>Down</button>
                                  </div>

                                  {isOptionQuestionType(question.type) && (
                                    <div className="options-list">
                                      <div className="panel-heading compact">
                                        <div>
                                          <h3>Options</h3>
                                          <p>Add choices users can select from.</p>
                                        </div>
                                        <button
                                          type="button"
                                          onClick={() => addOption(index)}
                                        >
                                          Add Option
                                        </button>
                                      </div>

                                      {(question.options || []).map((option, optionIndex) => (
                                        <input
                                          key={`${question.id}-option-${option.id || optionIndex}`}
                                          placeholder={`Option ${optionIndex + 1}`}
                                          value={getOptionText(option)}
                                          onChange={(event) =>
                                            updateOption(index, optionIndex, event.target.value)
                                          }
                                        />
                                      ))}

                                      {!(question.options || []).length && (
                                        <p className="muted">
                                          No options yet. Use Add Option to create the first choice.
                                        </p>
                                      )}
                                    </div>
                                  )}
                                </>
                              )}

                              {previewMode && renderPreviewQuestion(question)}
                            </>
                          )}
                        </SortableQuestion>
                      );
                    })}
                  </SortableContext>
                </DndContext>
              )}
            </section>
          </>
        ) : (
          isEditor && !loading && <section className="empty-state">Form not found.</section>
        )}
      </main>
    </div>
  );
}

function App() {
  const [authToken, setAuthToken] = useState(() => localStorage.getItem(AUTH_STORAGE_KEY));

  function logout() {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setAuthToken("");
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            authToken ? <Navigate to="/home" replace /> : <LoginPage onLogin={setAuthToken} />
          }
        />
        <Route
          path="/home"
          element={
            authToken ? <Builder authToken={authToken} onLogout={logout} /> : <Navigate to="/" replace />
          }
        />
        <Route
          path="/builder/:formId"
          element={
            authToken ? <Builder authToken={authToken} onLogout={logout} /> : <Navigate to="/" replace />
          }
        />
        <Route path="/form/:token" element={<PublicForm />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

