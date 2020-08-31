# Generates images; pass file name without extension
#. /home/ubuntu/instance
NAME=$1
SRC=$NAME.src
TEX=$NAME.tex
PNG=$NAME.png
PDF=$NAME.pdf
DVI=$NAME.dvi
EPS=$NAME.eps
cd $CACHE

grep -q -e "\\\\documentclass" $SRC

if [ "$?" -eq "0" ]; then
  cp $SRC $TEX
else
  grep -q '{tikzpicture}' $CACHE/$SRC
  if [ "$?" -eq "0" ]; then
    ./../templates/tikzcore $NAME > $TEX
  else
    ./../templates/tikz $NAME > $TEX
  fi
fi

pdflatex --jobname=$NAME $TEX && convert -density 300 $PDF $PNG
r=$?
pdftops $PDF $EPS
rm $DVI $NAME.log $NAME.aux >/dev/null 2>&1
chmod a+r $PDF
chmod a+r $PNG
exit $r
