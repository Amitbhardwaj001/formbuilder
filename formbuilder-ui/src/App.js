
import { useEffect, useState } from "react";
import { DndContext } from "@dnd-kit/core";
import {
  SortableContext,
  verticalListSortingStrategy,
  arrayMove,
} from "@dnd-kit/sortable";

function App() {
  const [forms, setForms] = useState([]);
  const [selectedForm, setSelectedForm] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [previewMode, setPreviewMode] = useState(false);
  const [tab, setTab] = useState("questions");

  const [analytics, setAnalytics] = useState({
    totalResponses: 0,
    questions: [],
  });

  useEffect(() => {
    fetch("http://localhost:8080/api/forms")
      .then((res) => res.json())
      .then((data) => setForms(data))
      .catch(console.error);
  }, []);

  function loadQuestions(form) {
    fetch(`http://localhost:8080/api/forms/${form.id}/questions`)
      .then((res) => res.json())
      .then((data) => {
        const enriched = data.map((q) => ({
          ...q,
          options: q.options || [],
          required: q.required || false,
        }));
        setQuestions(enriched);
      })
      .catch(console.error);
  }

  function saveQuestion(q) {
    if (!q.id) return;

    fetch(
      `http://localhost:8080/api/forms/question/${q.id}`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(q),
      }
    ).catch(console.error);
  }

  function saveQuestionOrder(updated) {
    updated.forEach((q, index) => {
      fetch(
        `http://localhost:8080/api/forms/question/${q.id}/order`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            displayOrder: index,
          }),
        }
      ).catch(console.error);
    });
  }

  function loadAnalytics() {
    if (!selectedForm) return;

    fetch(
      `http://localhost:8080/api/forms/analytics/${selectedForm.id}`
    )
      .then((r) => r.json())
      .then((data) => setAnalytics(data))
      .catch(console.error);
  }

  function publishForm() {
    if (!selectedForm) return;

    const url =
      `http://localhost:3000/form/${selectedForm.shareToken}`;

    navigator.clipboard.writeText(url);
    alert("Public URL copied");
  }

  function createForm() {
    fetch("http://localhost:8080/api/forms", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        title: "Untitled Form",
        description: "Created from UI",
      }),
    })
      .then((res) => res.json())
      .then((newForm) => {
        setForms([...forms, newForm]);
      });
  }

  function addQuestion(type = "TEXT") {
    if (!selectedForm) return;

    fetch(
      "http://localhost:8080/api/forms/question",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          text: "New Question",
          type,
          required:false,
          form: {
            id: selectedForm.id,
          },
        }),
      }
    )
      .then((res) => res.json())
      .then((newQuestion) => {
        newQuestion.required = false;
        newQuestion.options = [];
        const updated=[...questions,newQuestion];
        setQuestions(updated);
        saveQuestionOrder(updated);
      })
      .catch(() => {
        const updated=[
          ...questions,
          {
            id: Date.now(),
            text: "New Question",
            type,
            required:false,
            options:[],
          },
        ];
        setQuestions(updated);
      });
  }

  function deleteQuestion(index) {
    const q=questions[index];

    if(q.id){
      fetch(
       `http://localhost:8080/api/forms/question/${q.id}`,
       {
         method:"DELETE"
       }
      ).catch(console.error);
    }

    const updated=[...questions];
    updated.splice(index,1);
    setQuestions(updated);
    saveQuestionOrder(updated);
  }

  function duplicateQuestion(index) {
    const q=questions[index];

    fetch(
      "http://localhost:8080/api/forms/question",
      {
        method:"POST",
        headers:{
          "Content-Type":"application/json"
        },
        body:JSON.stringify({
          ...q,
          id:null,
          form:{id:selectedForm.id}
        })
      }
    )
    .then(r=>r.json())
    .then(newQ=>{
      const updated=[
        ...questions.slice(0,index+1),
        {...newQ,options:newQ.options||[]},
        ...questions.slice(index+1)
      ];

      setQuestions(updated);
      saveQuestionOrder(updated);
    })
    .catch(console.error);
  }

  function moveQuestionUp(index) {
    if(index===0) return;

    const updated=[...questions];

    [updated[index-1],updated[index]]=[
      updated[index],
      updated[index-1]
    ];

    setQuestions(updated);
    saveQuestionOrder(updated);
  }

  function moveQuestionDown(index) {
    if(index===questions.length-1) return;

    const updated=[...questions];

    [updated[index+1],updated[index]]=[
      updated[index],
      updated[index+1]
    ];

    setQuestions(updated);
    saveQuestionOrder(updated);
  }

  function handleDragEnd(event){
    const {active,over}=event;

    if(!over || active.id===over.id) return;

    const oldIndex=
      questions.findIndex(q=>q.id===active.id);

    const newIndex=
      questions.findIndex(q=>q.id===over.id);

    const reordered=arrayMove(
      questions,
      oldIndex,
      newIndex
    );

    setQuestions(reordered);
    saveQuestionOrder(reordered);
  }

  function addOption(qIndex){
    const updated=[...questions];

    if(!updated[qIndex].options){
      updated[qIndex].options=[];
    }

    updated[qIndex].options.push("New Option");
    setQuestions(updated);
    saveQuestion(updated[qIndex]);
  }

  return (
    <div style={{padding:40,fontFamily:"Arial"}}>
      <h1>Dynamic Form Builder</h1>

      <button onClick={createForm}>
        + Create Form
      </button>

      <button
        onClick={()=>setPreviewMode(!previewMode)}
        style={{marginLeft:15}}
      >
        Toggle Preview
      </button>

      <button
       onClick={publishForm}
       style={{marginLeft:15}}
      >
        Publish Form
      </button>

      <hr/>

      {forms.map((form)=>(
        <div
         key={form.id}
         onClick={()=>{
           setSelectedForm(form);
           loadQuestions(form);
         }}
         style={{
           border:"1px solid gray",
           padding:20,
           margin:"20px 0",
           cursor:"pointer"
         }}
        >
          <h2>{form.title}</h2>
          <p>{form.description}</p>
          <small>
            Share:
            http://localhost:3000/form/
            {form.shareToken}
          </small>
        </div>
      ))}

      {selectedForm && (
        <div style={{display:"flex",gap:30,marginTop:50}}>

          <div
           style={{
            width:220,
            border:"1px solid #ddd",
            padding:20,
            height:"fit-content"
           }}
          >
            <h3>Field Tools</h3>

            <button onClick={()=>addQuestion("TEXT")}>
              + Text
            </button>

            <br/><br/>

            <button onClick={()=>addQuestion("MCQ")}>
              + MCQ
            </button>

            <br/><br/>

            <button onClick={()=>addQuestion("CHECKBOX")}>
              + Checkbox
            </button>

            <br/><br/>

            <button onClick={()=>addQuestion("FILE")}>
              + File
            </button>

            <br/><br/>

            <button onClick={()=>addQuestion("SECTION")}>
              + Section
            </button>
          </div>

          <div
            style={{
             flex:1,
             border:"2px solid black",
             padding:30
            }}
          >

          <input
           value={selectedForm.title}
           onChange={(e)=>
            setSelectedForm({
             ...selectedForm,
             title:e.target.value
            })
           }
           style={{
             width:"100%",
             padding:10,
             fontSize:26
           }}
          />

          <p>Editing Form Builder</p>

          <button onClick={()=>setTab("questions")}>
            Questions
          </button>

          <button
           onClick={()=>{
             setTab("responses");
             loadAnalytics();
           }}
           style={{marginLeft:10}}
          >
            Responses
          </button>

          {tab==="questions" && (
            <button
             onClick={()=>addQuestion()}
             style={{marginLeft:20}}
            >
              + Add Question
            </button>
          )}

          {tab==="responses" && (
            <div
             style={{
              marginTop:30,
              border:"1px solid gray",
              padding:20
             }}
            >
              <h2>
               Responses:
               {analytics.totalResponses}
              </h2>

              {analytics.questions.map((q,i)=>(
                <div key={i} style={{marginTop:20}}>
                  <h3>{q.questionText}</h3>
                  <pre>
                    {JSON.stringify(
                     q.answerCount,
                     null,
                     2
                    )}
                  </pre>
                </div>
              ))}
            </div>
          )}

          {tab==="questions" && (
            <DndContext onDragEnd={handleDragEnd}>
            <SortableContext
              items={questions.map(q=>q.id)}
              strategy={verticalListSortingStrategy}
            >
              {questions.map((q,index)=>(
                <div
                  key={q.id}
                  style={{
                    marginTop:30,
                    border:"1px solid gray",
                    padding:20
                  }}
                >

                {!previewMode && (
                  <>
                  <div
                   style={{
                    fontSize:18,
                    marginBottom:10,
                    cursor:"grab"
                   }}
                  >
                    ☰ Drag Handle
                  </div>

                  <input
                   value={q.text}
                   onChange={(e)=>{
                    const updated=[...questions];
                    updated[index].text=e.target.value;
                    setQuestions(updated);
                    saveQuestion(updated[index]);
                   }}
                   style={{
                    width:"100%",
                    padding:10
                   }}
                  />

                  <br/><br/>

                  <select
                   value={q.type}
                   onChange={(e)=>{
                     const updated=[...questions];
                     updated[index].type=e.target.value;
                     setQuestions(updated);
                     saveQuestion(updated[index]);
                   }}
                  >
                    <option value="TEXT">Short Answer</option>
                    <option value="MCQ">Multiple Choice</option>
                    <option value="CHECKBOX">Checkbox</option>
                    <option value="DROPDOWN">Dropdown</option>
                    <option value="FILE">File Upload</option>
                    <option value="SECTION">Section Break</option>
                  </select>

                  <label style={{display:"block",marginTop:20}}>
                    <input
                     type="checkbox"
                     checked={q.required||false}
                     onChange={(e)=>{
                      const updated=[...questions];
                      updated[index].required=
                       e.target.checked;
                      setQuestions(updated);
                      saveQuestion(updated[index]);
                     }}
                    />
                    Required
                  </label>

                  <button
                   onClick={()=>duplicateQuestion(index)}
                   style={{marginLeft:10}}
                  >
                   Duplicate
                  </button>

                  <button
                   onClick={()=>deleteQuestion(index)}
                   style={{marginLeft:10}}
                  >
                   Delete
                  </button>

                  <button
                   onClick={()=>moveQuestionUp(index)}
                   style={{marginLeft:10}}
                  >
                   ↑
                  </button>

                  <button
                   onClick={()=>moveQuestionDown(index)}
                   style={{marginLeft:10}}
                  >
                   ↓
                  </button>

                  {(q.type==="MCQ" ||
                    q.type==="CHECKBOX" ||
                    q.type==="DROPDOWN") && (
                    <div style={{marginTop:20}}>

                    {(q.options||[]).map(
                     (op,opIndex)=>(
                      <input
                       key={opIndex}
                       value={op}
                       onChange={(e)=>{
                         const updated=[...questions];
                         updated[index].options[
                          opIndex
                         ]=e.target.value;
                         setQuestions(updated);
                         saveQuestion(updated[index]);
                       }}
                       style={{
                        display:"block",
                        marginTop:10,
                        padding:8,
                        width:"80%"
                       }}
                      />
                    ))}

                    <button
                     onClick={()=>addOption(index)}
                     style={{marginTop:15}}
                    >
                     + Add Option
                    </button>

                    </div>
                  )}

                  </>
                )}

                {previewMode && (
                  <div>
                    <h3>
                     {q.text}
                     {q.required && " *"}
                    </h3>

                    {q.type==="TEXT" && (
                      <input placeholder="Your answer"/>
                    )}

                    {q.type==="FILE" && (
                      <input type="file"/>
                    )}

                    {q.type==="DROPDOWN" && (
                      <select>
                       {(q.options||[]).map(
                        (op,i)=>(
                         <option key={i}>
                          {op}
                         </option>
                       ))}
                      </select>
                    )}

                    {(q.type==="MCQ" || q.type==="CHECKBOX") && (
                     <div>
                       {(q.options||[]).map(
                        (op,i)=>(
                         <div key={i}>
                          <input
                           type={
                             q.type==="MCQ"
                              ?"radio"
                              :"checkbox"
                           }
                          />
                          {op}
                         </div>
                       ))}
                     </div>
                    )}

                    {q.type==="SECTION" && (
                     <div>
                      --- New Page Section ---
                     </div>
                    )}
                  </div>
                )}

                </div>
              ))}
            </SortableContext>
            </DndContext>
          )}

          </div>
        </div>
      )}

    </div>
  );
}

export default App;
