{{/*
Expand the name of the chart.
*/}}
{{- define "dcms.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "dcms.fullname" -}}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "dcms.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
API selector labels
*/}}
{{- define "dcms.api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "dcms.name" . }}-api
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Frontend selector labels
*/}}
{{- define "dcms.frontend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "dcms.name" . }}-frontend
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
