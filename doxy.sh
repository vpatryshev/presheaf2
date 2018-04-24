# Generates pdf and png from xypic; pass file name without extension
# temporary name is dodoxy.sh; will be renamed back soon

. /home/ubuntu/instance

NAME=$1
SRC=$NAME.src
TEX=$NAME.tex
EPS=$NAME.eps
DVI=$NAME.dvi
IMG=$NAME.png
PDF=$NAME.pdf
cd $CACHE

if [ -r $IMG ] && { [ -r $PDF ]; }; then
  echo "Files exist"
  exit 0
fi

rm -f $TEX $EPS $DVI

chmod a+r $SRC 2>/dev/null || :

. ../templates/xy $NAME >$TEX
chmod a+r $TEX 2>/dev/null || :

/usr/bin/latex $TEX 
rlatex=$?

if [ $rlatex != 0 ]; then 
  echo "latex returned $rlatex"
  exit $rlatex
fi

echo "ok, ok"

/usr/bin/dvips -E -o $EPS $DVI && /usr/bin/epstopdf $EPS && /usr/bin/dvipng -T tight -o $IMG $DVI
r=$?

rm -f $EPS $DVI $NAME.log $NAME.aux
chmod a+r $IMG 2>/dev/null || :
chmod a+r $PDF 2>/dev/null || :
exit $r

