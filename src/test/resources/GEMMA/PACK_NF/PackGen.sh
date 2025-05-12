for NumCaisse in $(cut -f1 -d, ../../GEMMA/JDD_ARCHIVNF.csv|grep -v numCaisse) 
do
 unzip ARCHIVE_513_20250331000037.zip
 sed -i "s/513/${i}/g" DOC.txt signature.json

 for Package in JOUR_C513*
 do
  cp $Package $(echo $Package | sed "s/513/${NumCaisse}/g")
  rm -f $Package
  zip /apps/lmb/tmp/PACK_NF/ARCHIVE_${NumCaisse}_$(date +%Y%m%d)000000.zip JOUR*$NumCaisse*zip DOC.txt signature.json
  rm -f DOC.txt signature.json JOUR*$NumCaisse*zip
 done

done
