# Laboratoire 4 SYM

> Chris Barros, Vincent Canipel, Jérôme Arn

# Les capteurs

> Une  fois  la  manipulation effectuée,  vous  constaterez  que  les  animations  de  la  flèche  ne  sont  pas fluides,  il  va  y  avoir  un  tremblement plus  ou  moins  important même  si  le  téléphone  ne  bouge  pas. Veuillez expliquer quelle est la cause la plus probable de ce tremblement et donner une manière (sans forcément l’implémenter) d’y remédier.

On peut constater que la précision de certaines données vont jusqu'à six chiffres après la virgule. En faisant quelques tests avec le téléphone placé sur une table sans aucune interactions, nous avons pu constatez les valeurs suivantes pour le capteur Magnétomètre. Les valeurs démontrent que sans changer le téléphone de place, on a des valeurs changeante qui dont des "lags" sur la boussole.

| Axe X     | Axe Y     | Axe Z |
| --------- | --------- | ----- |
| 18.699997 | 14.300003 | -40.6 |
| 18.599998 | 14.199997 | -40.6 |
| 18.800003 | 14.199997 | -40.6 |

Une méthode qui permet d'atténuer ce problème est d'arrondir les valeurs des capteurs. Cela permet seulement d'atténuer car parfois les écarts font changer la valeur d'un paramètre d'une unité. Et cela sans bouger le téléphone. 

# Communication Bluetooth Low Energy

> La  caractéristique  permettant  de  lire  la  température  retourne  la  valeur  en  degrés  Celsius,multipliée par 10,sous la forme d’un entier non-signé de 16 bits. Quel est l’intérêt de procéder de  la  sorte?  Pourquoi  ne  pas  échanger  un nombre  à  virgule  flottante  de  type float par exemple?



> Le niveau de charge de la pile est à présent indiqué uniquement sur l’écran du périphérique, mais nous souhaiterions que celui-ci puisse informer le smartphone sur son niveau de charge restante.Veuillez spécifier la(les) caractéristique(s) qui composerai(en)t un tel service,mis à disposition par le périphérique et permettant de communiquer le niveau de batterie restant via Bluetooth  Low  Energy. Pour  chaque  caractéristique,  vous  indiquerez  les  opérations supportées (lecture, écriture, notification, indication, etc.) ainsi que les données échangées et leur format