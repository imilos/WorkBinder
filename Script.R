#Naziv fajla sa weights kao argument skript fajla
args=commandArgs(trailingOnly = TRUE)
weightsFileName=args[1]
weights= read.table(weightsFileName, header=FALSE)
w=data.matrix(weights,rownames.force=NA)
#Parametri proracuna
numberOfObjectives = 2
backtestingPeriod = 250
evaluationPeriod = 1000
alpha = 0.01
#################### Regulatory VaR #####################
#Ucitavanje vremenskih serija
d=read.table("timeSeries.csv-uploaded", header=FALSE, sep=',')
m=data.matrix(d,rownames.force=NA)[,-1]
#Racunanje vrednosti portfolija
#Vrednost holdingsa se racuna na osnovu capital invested weights i vrednosti aseta na poslednji dan evaluacije
timeSeriesRowCount = nrow(m)
assetCount = ncol(m)
shares=matrix(0,nrow=assetCount,ncol=1)
portfolioValue=1
for(i in 1:assetCount) shares[i]=w[i]*portfolioValue/m[timeSeriesRowCount,i]
p=m%*%shares
VaRestimations = matrix(0,backtestingPeriod+1,1)
secondPass = matrix(0,backtestingPeriod+1,1)
portfolioLength = nrow(p)
returnLength = portfolioLength-1
return=matrix(0,nrow=returnLength,ncol=1)
#Racunanje prinosa portfolija
for( i in 1:returnLength) {return[i,1]=p[i+1]/p[i]-1}
library(rugarch)
library(simsalapar)
#Inicijalni parametri
ar1=0
ma1=0
omega=0
alpha1=0
beta1=0
shape=0
correctionCounter=0
averageReturn=1000
capitalRequirements = 1000
DONE=TRUE
MESSAGE="OK"
j=1
while(j<=(backtestingPeriod+1))
{
r=matrix(return[(returnLength - evaluationPeriod - backtestingPeriod + j):(returnLength - backtestingPeriod + j - 1)],ncol=1)
if(j==(backtestingPeriod+1))
{
averageReturn=-mean(r)
}
if(j==1 || secondPass[j]==1)
{
sp<-ugarchspec (variance.model = list(model = 'sGARCH', garchOrder = c(1, 1), submodel = NULL, external.regressors = NULL, variance.targeting = FALSE), mean.model = list(armaOrder = c(1, 1), include.mean = FALSE, archm = FALSE, archpow = 1, arfima = FALSE, external.regressors = NULL, archex = FALSE), distribution.model = 'std', start.pars = list(), fixed.pars = list())
}
else
{
sp<-ugarchspec (variance.model = list(model = 'sGARCH', garchOrder = c(1, 1), submodel = NULL, external.regressors = NULL, variance.targeting = FALSE), mean.model = list(armaOrder = c(1, 1), include.mean = FALSE, archm = FALSE, archpow = 1, arfima = FALSE, external.regressors = NULL, archex = FALSE), distribution.model = 'std', start.pars = list(ar1=ar1,ma1=ma1,omega=omega,alpha1=alpha1,beta1=beta1,shape=shape), fixed.pars = list())
}
#Fitovanje GARCH modela
tryObject=tryCatch.W.E(ugarchfit(data = r, spec = sp))
if(inherits(tryObject$value, "error") || is.null(coef(tryObject$value)['ar1'])){
if(secondPass[j]==0){
secondPass[j]=1
}else{
DONE=FALSE
if(!is.null(tryObject$warning)){
MESSAGE=tryObject$warning$message
}
else if(!is.null(tryObject$error)){
MESSAGE=tryObject$error$message
}
break
}
}else{
fit = tryObject$value
ar1=coef(fit)['ar1']
ma1=coef(fit)['ma1']
omega=coef(fit)['omega']
alpha1=coef(fit)['alpha1']
beta1=coef(fit)['beta1']
shape=coef(fit)['shape']
#Predikcija VaRa
tryObject2=tryCatch.W.E(ugarchforecast(fit, n.ahead=1))
if(inherits(tryObject2$value, "error")){
DONE=FALSE
MESSAGE=tryObject2$value$message
break
}else{
forc = tryObject2$value
sigma=sigma(forc)
quantil= qdist('std', p= alpha, mu = 0, sigma = 1, shape=shape)
#Odredjivanje VaR-a
VaR = sigma*quantil
VaRestimations[j,1]=VaR
j<-j+1
}
}
}
regulatoryVaR = 0
stressVaR = 0
if(DONE){
violationsCounter = 0
averageRisk = 0.0
averageRiskCalculationPeriod = 60
for (i in 1:backtestingPeriod)
{
if(return[returnLength-backtestingPeriod+i] < VaRestimations[i]) violationsCounter = violationsCounter + 1
}
for (i in 1:averageRiskCalculationPeriod)
{
averageRisk = averageRisk + VaRestimations[backtestingPeriod - averageRiskCalculationPeriod + i]
}
averageRisk = averageRisk / averageRiskCalculationPeriod
k = 0.0
if(violationsCounter <= 4) {
k = 0.0
} else if (violationsCounter == 5) {
k = 0.4
} else if (violationsCounter == 6) {
k = 0.5
} else if (violationsCounter == 7) {
k = 0.65
} else if (violationsCounter == 8) {
k = 0.75
} else if (violationsCounter == 9) {
k = 0.85
} else k = 1
Comp1 = abs(VaRestimations[backtestingPeriod+1,1])
Comp2 = abs((3 + k) * averageRisk)
regulatoryVaR = max(c(Comp1, Comp2)) * sqrt(10)
}
#################### End Regulatory VaR #####################
if(DONE){
capitalRequirements = regulatoryVaR + stressVaR
}
{
cat("OK
")
cat(numberOfObjectives, "
")
cat(averageReturn, "
")
cat(violationsCounter, "
")
}
