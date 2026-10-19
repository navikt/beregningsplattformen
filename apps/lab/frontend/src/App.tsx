import "@navikt/ds-css";
import "./index.css";
import ReactDOM from "react-dom/client";
import React from "react";
import Lab from "./pages/Lab";
import { initApm } from "./util/apm";

initApm();

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <React.StrictMode>
        <div className="page-wrapper">
            <div className="page-layout">
                <main>
                    <Lab />
                </main>
            </div>
        </div>
    </React.StrictMode>
);
