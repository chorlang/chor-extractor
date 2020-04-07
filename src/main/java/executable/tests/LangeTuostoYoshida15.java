package executable.tests;


import executable.tests.TestUtil;

public class LangeTuostoYoshida15 {

    public void runningExample(){
        TestUtil.runExtractionTest(
                "a {def Y {c?; d!<free>; X} def X {if e then b+win; c+lose; b?; Y else b+lose; c+win; b?; Y} main {X}} |" +
                        "b {def X {a&{win: a!<sig>; X, lose: a!<sig>; X}} main {X}} |" +
                        "c {def X {d!<busy>; a&{win: a!<msg>; X, lose: a!<msg>; X}} main {X}} |" +
                        "d {def X {c?; a?; X} main {X}}" );
    }

    public void runningExample2x(){
        TestUtil.runExtractionTest(
                "a1 {def X {if e then b1+win; c1+lose; b1?; c1?; d1!<free>; X else b1+lose; c1+win; b1?; c1?; d1!<free>; X} main {X}} |" +
                        "b1 {def X {a1&{win: c1!<lose>; a1!<sig>; X, lose: c1?; a1!<sig>; X}} main {X}} |" +
                        "c1 {def X {d1!<busy>; a1&{win: b1!<lose>; a1!<msg>; X, lose: b1?; a1!<msg>; X}} main {X}} |" +
                        "d1 {def X {c1?; a1?; X} main {X}} | " +
                        "a2 {def X {if e then b2+win; c2+lose; b2?; c2?; d2!<free>; X else b2+lose; c2+win; b2?; c2?; d2!<free>; X} main {X}} |" +
                        "b2 {def X {a2&{win: c2!<lose>; a2!<sig>; X, lose: c2?; a2!<sig>; X}} main {X}} |" +
                        "c2 {def X {d2!<busy>; a2&{win: b2!<lose>; a2!<msg>; X, lose: b2?; a2!<msg>; X}} main {X}} |" +
                        "d2 {def X {c2?; a2?; X} main {X}}" );
    }

    public void bargain(){
        TestUtil.runExtractionTest(
                "a { def X {if notok then b+hag; b?; X else b+happy; c!<info>; stop} main {X}} | " +
                        "b { def Y {a&{hag: a!<price>; Y, happy: stop}} main {Y}} | " +
                        "c { main {a?; stop}}", "c" );
    }

    public void bargain2x(){
        TestUtil.runExtractionTest(
                "a { def X {b!<hag>; b?; if price then b+deal; b!<happy>; c!<info>; X else b+nodeal; X} main {X}} | " +
                        "b { def Y {a?; a!<price>; a&{deal: a?; Y, nodeal: Y}} main {Y}} | " +
                        "c { def Z {a?; Z} main {Z}} | " +
                        "d { def X {e!<hag>; e?; if price then e+deal; e!<happy>; f!<info>; X else e+nodeal; X} main {X}} | " +
                        "e { def Y {d?; d!<price>; d&{deal: d?; Y, nodeal: Y}} main {Y}} | " +
                        "f { def Z {d?; Z} main {Z}}", "c", "f" );
    }

    public void health(){
        TestUtil.runExtractionTest(
                "hs{def X{p?; ss!<subscribed>; ss&{" +
                        "ok: p+subscribed; as!<account>; as?; t!<fwd>; t?; X, " +
                        "nok: p+notSubscribed; X}} main{X}} | " +
                        "p{def X{hs!<sendData>; hs&{subscribed: es?; X, notSubscribed: X}} main{X}} | " +
                        "ss{def X{hs?; if ok then hs+ok; X else hs+nok; X} main{X}} | " +
                        "as{def X{hs?; hs!<logCreated>; X} main{X}} | " +
                        "t{def X{hs?; hs!<fwdOk>; es!<helpReq>; X} main{X}} | " +
                        "es{def X{t?; p!<provideService>; X} main{X}}", "as", "t", "es" );
    }

    public void health2x(){
        TestUtil.runExtractionTest(
                "hs{def X{p?; ss!<subscribed>; ss&{" +
                        "ok: p+subscribed; as!<account>; as?; t!<fwd>; t?; X, " +
                        "nok: p+notSubscribed; X}} main{X}} | " +
                        "p{def X{hs!<sendData>; hs&{subscribed: es?; X, notSubscribed: X}} main{X}} | " +
                        "ss{def X{hs?; if ok then hs+ok; X else hs+nok; X} main{X}} | " +
                        "as{def X{hs?; hs!<logCreated>; X} main{X}} | " +
                        "t{def X{hs?; hs!<fwdOk>; es!<helpReq>; X} main{X}} | " +
                        "es{def X{t?; p!<provideService>; X} main{X}} | " +
                        "hs2{def X{p2?; ss2!<subscribed>; ss2&{" +
                        "ok: p2+subscribed; as2!<account>; as2?; t2!<fwd>; t2?; X, " +
                        "nok: p2+notSubscribed; X}} main{X}} | " +
                        "p2{def X{hs2!<sendData>; hs2&{subscribed: es2?; X, notSubscribed: X}} main{X}} | " +
                        "ss2{def X{hs2?; if ok then hs2+ok; X else hs2+nok; X} main{X}} | " +
                        "as2{def X{hs2?; hs2!<logCreated>; X} main{X}} | " +
                        "t2{def X{hs2?; hs2!<fwdOk>; es2!<helpReq>; X} main{X}} | " +
                        "es2{def X{t2?; p2!<provideService>; X} main{X}}", "as", "t", "es", "as2", "t2", "es2" );
    }

    public void filter(){
        TestUtil.runExtractionTest(
                "filter {" +
                        "def X {data!<newFilterRequest>; Y} " +
                        "def Y {data&{" +
                        "item: data?; if itemToBeFiltered then data!<ok>; Y else data!<remove>; Y," +
                        "noItem: X}}" +
                        "main {X}} | " +
                        "data {" +
                        "def X {filter?; Y} " +
                        "def Y {if itemToBeFiltered " +
                        "then filter+item; filter!<itemToBeFiltered>; filter?; Y " +
                        "else filter+noItem; X} " +
                        "main {X}}" );
    }

    public void filter2x(){
        TestUtil.runExtractionTest(
                "filter1 {" +
                        "def X {data1!<newFilterRequest>; Y} " +
                        "def Y {data1&{" +
                        "item: data1?; if itemToBeFiltered then data1!<ok>; Y else data1!<remove>; Y," +
                        "noItem: data1?; X}}" +
                        "main {X}} | " +
                        "data1 {" +
                        "def X {filter1?; Y} " +
                        "def Y {if itemToBeFiltered " +
                        "then filter1+item; filter1!<itemToBeFiltered>; filter1?; Y " +
                        "else filter1+noItem; filter1!<noMoreItems>; X} " +
                        "main {X}} | " +
                        "filter2 {" +
                        "def X {data2!<newFilterRequest>; Y} " +
                        "def Y {data2&{" +
                        "item:  data2?; if itemToBeFiltered then data2!<ok>; Y else data2!<remove>; Y," +
                        "noItem: data2?; X}}" +
                        "main {X}} | " +
                        "data2 {" +
                        "def X {filter2?; Y} " +
                        "def Y {if itemToBeFiltered " +
                        "then filter2+item; filter2!<itemToBeFiltered>; filter2?; Y " +
                        "else filter2+noItem; filter2!<noMoreItems>; X} " +
                        "main {X}}" );
    }

    public void logistic(){
        TestUtil.runExtractionTest(
                "supplier {" +
                        "def X {shipper?; Y} " +
                        "def Y {if needToShip " +
                        "then shipper+item; X " +
                        "else shipper+done; retailer!<UpdatePOandDeliverySchedule>; retailer?; retailer?; retailer!<FinalizedPOandDeliverySchedule>; stop}" +
                        "main { retailer!<PlannedOrderVariations>; retailer?; retailer?; Y}" + "} | " +
                        "retailer {" +
                        "main {" +
                        "supplier?; supplier!<OrderDeliveryVariations>; supplier!<DeliverCheckPointRequest>; " +
                        "supplier?; supplier!<POandDeliveryScheduleMods>; shipper!<ConfirmationofDeliverySchedule>; " +
                        "supplier!<AcceptPOandDeliverySchedule>; supplier?; stop}} |" +
                        "shipper {" +
                        "def X{supplier!<DeliveryItem>; Y} " +
                        "def Y {supplier&{item: X, done: retailer?; stop}}" +
                        "main{Y}}", "retailer" );
    }

    public void logistic2x(){
        TestUtil.runExtractionTest(
                "supplier {" +
                        "def X {shipper?; Y} " +
                        "def Y {if needToShip " +
                        "then shipper+item; X " +
                        "else shipper+done; retailer!<UpdatePOandDeliverySchedule>; retailer?; retailer?; retailer!<FinalizedPOandDeliverySchedule>; stop}" +
                        "main { retailer!<PlannedOrderVariations>; retailer?; retailer?; Y}" + "} | " +
                        "retailer {" +
                        "main {" +
                        "supplier?; supplier!<OrderDeliveryVariations>; supplier!<DeliverCheckPointRequest>; " +
                        "supplier?; supplier!<POandDeliveryScheduleMods>; shipper!<ConfirmationofDeliverySchedule>; " +
                        "supplier!<AcceptPOandDeliverySchedule>; supplier?; stop}} |" +
                        "shipper {" +
                        "def X{supplier!<DeliveryItem>; Y} " +
                        "def Y {supplier&{item: X, done: retailer?; stop}}" +
                        "main{Y}} | " +
                        "supplier2 {" +
                        "def X {shipper2?; Y} " +
                        "def Y {if needToShip " +
                        "then shipper2+item; X " +
                        "else shipper2+done; retailer2!<UpdatePOandDeliverySchedule>; retailer2?; retailer2?; retailer2!<FinalizedPOandDeliverySchedule>; stop}" +
                        "main { retailer2!<PlannedOrderVariations>; retailer2?; retailer2?; Y}" + "} | " +
                        "retailer2 {" +
                        "main {" +
                        "supplier2?; supplier2!<OrderDeliveryVariations>; supplier2!<DeliverCheckPointRequest>; " +
                        "supplier2?; supplier2!<POandDeliveryScheduleMods>; shipper2!<ConfirmationofDeliverySchedule>; " +
                        "supplier2!<AcceptPOandDeliverySchedule>; supplier2?; stop}} |" +
                        "shipper2 {" +
                        "def X{supplier2!<DeliveryItem>; Y} " +
                        "def Y {supplier2&{item: X, done: retailer2?; stop}}" +
                        "main{Y}}", "retailer", "retailer2");
    }

    public void logistic2(){
        TestUtil.runExtractionTest(
                "supplier {" +
                        "def X {shipper?; consignee?; Y} " +
                        "def Y {if needToShip " +
                        "then shipper+item; consignee+item; X " +
                        "else shipper+done; consignee+done; " +
                        "retailer!<UpdatePOandDeliverySchedule>; retailer?; retailer?; retailer!<FinalizedPOandDeliverySchedule>; stop}" +
                        "main { retailer!<PlannedOrderVariations>; retailer?; retailer?; Y}" +
                        "} | " +
                        "retailer {main {" +
                        "supplier?; supplier!<OrderDeliveryVariations>; supplier!<DeliverCheckPointRequest>; " +
                        "supplier?; supplier!<POandDeliveryScheduleMods>; shipper!<ConfirmationofDeliverySchedule>; " +
                        "supplier!<AcceptPOandDeliverySchedule>; supplier?; stop}} |" +
                        "consignee {" +
                        "def X{supplier!<DeliveryItem>; Z} " +
                        "def Z {supplier&{item: X, done: stop}}" +
                        "main{Z}} | " +
                        "shipper {" +
                        "def X{supplier!<DeliveryItem>; Z} " +
                        "def Z {supplier&{item: X, done: retailer?; stop}}" +
                        "main{Z}}", "retailer" );
    }

    public void cloudSystem(){
        TestUtil.runExtractionTest(
                "cl{" +
                        "def X{int!<connect>; int?; Y} " +
                        "def Y{if access then appli+awaitcl; appli!<access>; Y else int!<logout>; appli+syncLogout; appli?; X} " +
                        "main {X}} | " +
                        "appli{" +
                        "def X{int?; Y} " +
                        "def Y{cl&{awaitcl: cl?; Y, syncLogout: db!<log>; cl!<syncLog>; X}} " +
                        "main {X}} | " +
                        "int{" +
                        "def X{cl?; appli!<setup>; cl!<syncAccess>; cl?; X} " +
                        "main {X}} | " +
                        "db{" +
                        "def X{appli?; X} " +
                        "main {X}}", "db", "int" );
    }

    public void cloudSystem2x(){
        TestUtil.runExtractionTest(
        "cl{"+
        "def X{int!<connect>; int?; Y} "+
        "def Y{if access then appli+awaitcl; appli!<access>; Y else int!<logout>; appli+syncLogout; appli?; X} "+
        "main {X}} | "+
        "appli{"+
        "def X{int?; Y} "+
        "def Y{cl&{awaitcl: cl?; Y, syncLogout: db!<log>; cl!<syncLog>; X}} "+
        "main {X}} | "+
        "int{"+
        "def X{cl?; appli!<setup>; cl!<syncAccess>; cl?; X} "+
        "main {X}} | "+
        "db{"+
        "def X{appli?; X} "+
        "main {X}} | "+
        "cl2{"+
        "def X{int2!<connect>; int2?; Y} "+
        "def Y{if access then appli2+awaitcl; appli2!<access>; Y else int2!<logout>; appli2+syncLogout; appli2?; X} "+
        "main {X}} | "+
        "appli2{"+
        "def X{int2?; Y} "+
        "def Y{cl2&{awaitcl: cl2?; Y, syncLogout: db2!<log>; cl2!<syncLog>; X}} "+
        "main {X}} | "+
        "int2{"+
        "def X{cl2?; appli2!<setup>; cl2!<syncAccess>; cl2?; X} "+
        "main {X}} | "+
        "db2{"+
        "def X{appli2?; X} "+
        "main {X}}","db","int","db2","int2");
    }

    public void sanitaryAgency(){
        TestUtil.runExtractionTest(
        "citizen{"+
        "def X{"+
        "sanagency!<request>; sanagency?; sanagency!<provInf>; sanagency&{"+
        "refusal: X, "+
        "acceptance: coop?; bank!<paymentPrivateFee>; X}} "+
        "main{X}"+
        "} | "+
        "sanagency{"+
        "def X{"+
        "citizen?; citizen!<askInfo>; citizen?; if infoProved "+
        "then citizen+acceptance; coop!<req>; bank!<paymentPublicFee>; bank?; X "+
        "else citizen+refusal; X }"+
        "main {X}} | "+
        "coop{def X{"+
        "sanagency?; "+
        "if fine "+
        "then citizen!<provT>; bank+recMoneyPossT; bank?; X "+
        "else citizen!<provM>; bank+recMoneyPossM; bank?; X} "+
        "main{X}} | "+
        "bank{"+
        "def X{ coop&{"+
        "recMoneyPossT: coop!<paymentT>; Y, "+
        "recMoneyPossM: coop!<paymentM>; Y}} "+
        "def Y{citizen?; sanagency?; sanagency!<done>; X} "+
        "main{X}}","coop","bank");
    }

    public void sanitaryAgency2x(){
        TestUtil.runExtractionTest(
                "citizen{" +
                        "def X{" +
                        "sanagency!<request>; sanagency?; sanagency!<provInf>; sanagency&{" +
                        "refusal: X, " +
                        "acceptance: coop?; bank!<paymentPrivateFee>; X}} " +
                        "main{X}" +
                        "} | " +
                        "sanagency{" +
                        "def X{" +
                        "citizen?; citizen!<askInfo>; citizen?; if infoProved " +
                        "then citizen+acceptance; coop!<req>; bank!<paymentPublicFee>; bank?; X " +
                        "else citizen+refusal; X }" +
                        "main {X}} | " +
                        "coop{def X{" +
                        "sanagency?; " +
                        "if fine " +
                        "then citizen!<provT>; bank+recMoneyPossT; bank?; X " +
                        "else citizen!<provM>; bank+recMoneyPossM; bank?; X} " +
                        "main{X}} | " +
                        "bank{" +
                        "def X{ coop&{" +
                        "recMoneyPossT: coop!<paymentT>; Y, " +
                        "recMoneyPossM: coop!<paymentM>; Y}} " +
                        "def Y{citizen?; sanagency?; sanagency!<done>; X} " +
                        "main{X}} | " +
                        "citizen2{" +
                        "def X{" +
                        "sanagency2!<request>; sanagency2?; sanagency2!<provInf>; sanagency2&{" +
                        "refusal: X, " +
                        "acceptance: coop2?; bank2!<paymentPrivateFee>; X}} " +
                        "main{X}" +
                        "} | " +
                        "sanagency2{" +
                        "def X{" +
                        "citizen2?; citizen2!<askInfo>; citizen2?; if infoProved " +
                        "then citizen2+acceptance; coop2!<req>; bank2!<paymentPublicFee>; bank2?; X " +
                        "else citizen2+refusal; X }" +
                        "main {X}} | " +
                        "coop2{def X{" +
                        "sanagency2?; " +
                        "if fine " +
                        "then citizen2!<provT>; bank2+recMoneyPossT; bank2?; X " +
                        "else citizen2!<provM>; bank2+recMoneyPossM; bank2?; X} " +
                        "main{X}} | " +
                        "bank2{" +
                        "def X{ coop2&{" +
                        "recMoneyPossT: coop2!<paymentT>; Y, " +
                        "recMoneyPossM: coop2!<paymentM>; Y}} " +
                        "def Y{citizen2?; sanagency2?; sanagency2!<done>; X} " +
                        "main{X}}", "coop", "bank", "coop2", "bank2" );
    }
}