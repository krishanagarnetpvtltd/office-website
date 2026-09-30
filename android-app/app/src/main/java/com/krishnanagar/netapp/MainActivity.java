package com.krishnanagar.netapp;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    static final int BLUE = Color.rgb(8,119,232), NAVY = Color.rgb(7,27,73), GREEN = Color.rgb(19,184,91);
    static final int REQ_LOCATION = 501, REQ_NOTIF = 502;
    static final String PREFS = "kn_prefs";
    static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    Db db;
    LinearLayout root, content;
    TextView title;
    String role = "", currentPhone = "", currentName = "";
    double myLat = Double.NaN, myLng = Double.NaN;
    int radiusKm = 5;
    LinearLayout nearbyList;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);
        seedStaff();
        requestNotificationPermission();
        showWelcome();
    }

    void base(String pageTitle) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(246,249,254));
        LinearLayout bar = new LinearLayout(this); bar.setPadding(20,18,20,18); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setBackgroundColor(NAVY);
        title = text(pageTitle,20,Color.WHITE); bar.addView(title,new LinearLayout.LayoutParams(0,WRAP,1));
        if (!role.isEmpty()) { TextView logout=text("Logout",14,Color.WHITE); logout.setOnClickListener(v->{clearSession(); showWelcome();}); bar.addView(logout,new LinearLayout.LayoutParams(WRAP,WRAP)); }
        root.addView(bar);
        ScrollView sv=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(18,18,18,28); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(MATCH,MATCH));
        setContentView(root);
    }

    void showWelcome() {
        role=""; currentPhone=""; currentName="";
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("staffActive",false).apply();
        base("Krishnanagar Net");
        ImageView logo=new ImageView(this); logo.setImageResource(android.R.drawable.sym_def_app_icon); logo.setBackgroundColor(NAVY);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(110,110); lp.gravity=Gravity.CENTER_HORIZONTAL; content.addView(logo,lp);
        TextView h=text("Connecting You to a World of Possibilities.",26,NAVY); h.setGravity(Gravity.CENTER); content.addView(h,new LinearLayout.LayoutParams(MATCH,WRAP));
        content.addView(spacer(18));
        content.addView(cardLabel("Choose your access"));
        Button c=button("Customer Login / Register",BLUE); c.setOnClickListener(v->showAuth("customer")); content.addView(c);
        Button s=button("Staff / Helpdesk Login",GREEN); s.setOnClickListener(v->showAuth("staff")); content.addView(s);
        content.addView(spacer(18));
        content.addView(note("Demo staff account: 9811940740 / Staff@1234"));
        content.addView(note("The APK works standalone for testing. Live CMS, RADIUS/OLT and cloud push integration are added later."));
    }

    void showAuth(String r) {
        role=r; base(r.equals("staff")?"Staff / Helpdesk Login":"Customer Login");
        EditText phone=field("Mobile number"); EditText pass=field("Password"); pass.setInputType(0x81);
        content.addView(phone); content.addView(pass);
        EditText name=null;
        if(r.equals("customer")){
            name=field("Full name (for new registration)");
            content.addView(name);
        }
        final EditText nameRef=name;
        Button login=button("Login",BLUE); content.addView(login);
        Button reg=null;
        if(r.equals("customer")) { reg=button("Create Account",GREEN); content.addView(reg); }
        TextView back=text("← Back",14,NAVY); back.setPadding(0,24,0,4); content.addView(back);
        login.setOnClickListener(v->{
            String p=phone.getText().toString().trim(); String pw=pass.getText().toString();
            if(p.isEmpty()||pw.isEmpty()){toast("Enter mobile and password");return;}
            if(r.equals("staff")) {
                if(p.equals("9811940740") && pw.equals("Staff@1234")) { currentPhone=p; currentName="Krishnanagar Staff"; staffDashboard(); }
                else toast("Invalid staff login");
            } else {
                String saved=db.getUserPassword(p);
                if(saved!=null && saved.equals(hash(pw))){currentPhone=p;currentName=db.getUserName(p);customerDashboard();}
                else toast("Account not found or password incorrect. Register first.");
            }
        });
        if(reg!=null) reg.setOnClickListener(v->{
            String n=nameRef.getText().toString().trim(), p=phone.getText().toString().trim(), pw=pass.getText().toString();
            if(n.isEmpty()||p.isEmpty()||pw.length()<6){toast("Enter name, mobile and password (6+ characters)");return;}
            if(db.userExists(p)){toast("Account already exists");return;}
            String id=db.createUser(n,p,hash(pw)); new AlertDialog.Builder(this).setTitle("Account created").setMessage("Customer ID: "+id).setPositiveButton("Continue",(d,w)->{currentPhone=p;currentName=n;customerDashboard();}).show();
        });
        back.setOnClickListener(v->showWelcome());
    }

    void customerDashboard() {
        role="customer";
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("staffActive",false).apply();
        base("Customer Dashboard");

        content.addView(card("Welcome, "+currentName+""));
        LinearLayout stats=new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        int open=db.countCustomerOpenTickets(currentPhone);
        stats.addView(statBox("Plan","100 Mbps",BLUE),new LinearLayout.LayoutParams(0,WRAP,1));
        stats.addView(statBox("Open Tickets",String.valueOf(open),GREEN),new LinearLayout.LayoutParams(0,WRAP,1));
        content.addView(stats);

        content.addView(info("Customer ID",db.getCustomerId(currentPhone)));
        content.addView(info("Service Status","Active"));

        content.addView(sectionTitle("Quick actions"));
        Button service=button("My Internet Service",BLUE); service.setOnClickListener(v->showServiceDetails()); content.addView(service);
        Button complaint=button("Raise New Complaint",GREEN); complaint.setOnClickListener(v->raiseCustomerComplaint()); content.addView(complaint);
        Button tickets=button("My Complaints & Ticket History",NAVY); tickets.setOnClickListener(v->showMyTickets()); content.addView(tickets);
        Button troubleshoot=button("Run Internet Troubleshooter",Color.rgb(120,80,200)); troubleshoot.setOnClickListener(v->runTroubleshooter()); content.addView(troubleshoot);
        Button support=button("Contact Support",Color.DKGRAY); support.setOnClickListener(v->showSupport()); content.addView(support);
        Button profile=button("My Profile",Color.rgb(70,90,120)); profile.setOnClickListener(v->showCustomerProfile()); content.addView(profile);

        content.addView(note("This build stores accounts and tickets locally on the phone. Live customer, plan, billing and ticket synchronization with your CMS is not connected yet."));
    }

    void showServiceDetails(){
        base("My Internet Service");
        content.addView(card("Service overview"));
        content.addView(info("Customer ID",db.getCustomerId(currentPhone)));
        content.addView(info("Internet Plan","100 Mbps"));
        content.addView(info("Service Status","Active"));
        content.addView(info("Connection Area","Krishnanagar service area"));
        content.addView(note("Plan speed and service status are demo values in the standalone APK. These should be read from your CMS in the production integration."));
        Button trouble=button("Run Connection Test",BLUE); trouble.setOnClickListener(v->runTroubleshooter()); content.addView(trouble);
        Button back=button("Back to Dashboard",Color.GRAY); back.setOnClickListener(v->customerDashboard()); content.addView(back);
    }

    void showSupport(){
        base("Support Center");
        content.addView(card("Krishnanagar Net Support"));
        content.addView(note("Use the support line for urgent internet problems. You can also create a complaint and track it from the app."));
        Button c1=button("Call 9867400888",BLUE); c1.setOnClickListener(v->dial("9867400888")); content.addView(c1);
        Button c2=button("Call 9811940740",GREEN); c2.setOnClickListener(v->dial("9811940740")); content.addView(c2);
        Button raise=button("Raise a Complaint",NAVY); raise.setOnClickListener(v->raiseCustomerComplaint()); content.addView(raise);
        Button back=button("Back",Color.GRAY); back.setOnClickListener(v->customerDashboard()); content.addView(back);
    }

    void showCustomerProfile(){
        base("My Profile");
        content.addView(card("Account details"));
        content.addView(info("Name",currentName));
        content.addView(info("Mobile",currentPhone));
        content.addView(info("Customer ID",db.getCustomerId(currentPhone)));
        Button tickets=button("View My Ticket History",NAVY); tickets.setOnClickListener(v->showMyTickets()); content.addView(tickets);
        Button back=button("Back",Color.GRAY); back.setOnClickListener(v->customerDashboard()); content.addView(back);
    }

    void raiseCustomerComplaint(){
        myLat=Double.NaN; myLng=Double.NaN;
        base("Raise Complaint");
        EditText msg=multi("Describe your issue");
        Spinner cat=new Spinner(this); String[] cats={"No Internet","Slow Speed","Frequent Disconnection","Router / ONU","Billing","Other"}; cat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));
        content.addView(label("Issue type")); content.addView(cat); content.addView(msg);
        TextView loc=text("Location: not attached",13,Color.GRAY); content.addView(loc);
        Button attach=button("Attach Current Location",BLUE); attach.setOnClickListener(v->getLocation(false,loc)); content.addView(attach);
        Button submit=button("Submit Complaint",GREEN); submit.setOnClickListener(v->{
            String m=msg.getText().toString().trim(); if(m.isEmpty()){toast("Describe the problem");return;}
            long id=db.addTicket(currentPhone,currentName,cat.getSelectedItem().toString(),m,myLat,myLng,"customer");
            notifyUser("Complaint submitted","#"+id+" has been registered.");
            new AlertDialog.Builder(this).setTitle("Complaint submitted").setMessage("Ticket #"+id+" created successfully.").setPositiveButton("View Tickets",(d,w)->showMyTickets()).show();
        }); content.addView(submit);
        Button back=button("← Back",Color.GRAY); back.setOnClickListener(v->customerDashboard()); content.addView(back);
    }

    void showMyTickets(){
        base("My Complaints");
        List<Ticket> list=db.getCustomerTickets(currentPhone);
        TextView summary=note(list.size()+" ticket(s) in your account."); content.addView(summary);
        if(list.isEmpty()){
            content.addView(card("No complaints yet"));
            content.addView(note("When you report an internet problem, the ticket will appear here with its current status and staff updates."));
            Button raise=button("Raise Complaint",GREEN); raise.setOnClickListener(v->raiseCustomerComplaint()); content.addView(raise);
        } else {
            for(Ticket t:list) content.addView(ticketCard(t,false));
        }
        Button back=button("Back to Dashboard",Color.GRAY); back.setOnClickListener(v->customerDashboard()); content.addView(back);
    }

    void staffDashboard(){
        role="staff";
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("staffActive",true).apply();
        base("Staff / Helpdesk");

        content.addView(card("Krishnanagar Net Helpdesk"));

        LinearLayout a=new LinearLayout(this); a.setOrientation(LinearLayout.HORIZONTAL);
        int newCount=db.countTicketsByStatus("New");
        int assignedCount=db.countTicketsByStatus("Assigned");
        int progressCount=db.countTicketsByStatus("In Progress");
        a.addView(statBox("New",String.valueOf(newCount),BLUE),new LinearLayout.LayoutParams(0,WRAP,1));
        a.addView(statBox("Assigned",String.valueOf(assignedCount),GREEN),new LinearLayout.LayoutParams(0,WRAP,1));
        a.addView(statBox("In Progress",String.valueOf(progressCount),Color.rgb(190,120,20)),new LinearLayout.LayoutParams(0,WRAP,1));
        content.addView(a);

        sectionTitle("Complaint management");
        Button all=button("All Complaints",BLUE); all.setOnClickListener(v->showAllComplaints()); content.addView(all);
        Button nearby=button("Nearby Complaints",GREEN); nearby.setOnClickListener(v->showNearbyComplaints()); content.addView(nearby);
        Button search=button("Search Customer",NAVY); search.setOnClickListener(v->showCustomerSearch()); content.addView(search);
        Button create=button("Create Complaint for Customer",Color.rgb(120,80,200)); create.setOnClickListener(v->staffCreateTicket()); content.addView(create);

        sectionTitle("Nearby alert preview");
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        Spinner radius=new Spinner(this); ArrayList<String> rs=new ArrayList<>(); for(int x:new int[]{2,5,10,25})rs.add(x+" km");
        radius.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,rs)); radius.setSelection(1);
        radius.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?>p){} public void onItemSelected(android.widget.AdapterView<?>p,View v,int pos,long id){radiusKm=new int[]{2,5,10,25}[pos];}});
        row.addView(radius,new LinearLayout.LayoutParams(0,WRAP,1));
        Button loc=button("Update My Location",Color.DKGRAY); row.addView(loc,new LinearLayout.LayoutParams(0,WRAP,1));
        content.addView(row);
        loc.setOnClickListener(v->getStaffLocationAndRefresh());

        nearbyList=new LinearLayout(this); nearbyList.setOrientation(LinearLayout.VERTICAL); content.addView(nearbyList);
        loadNearby();

        Button refresh=button("Refresh Dashboard",NAVY); refresh.setOnClickListener(v->staffDashboard()); content.addView(refresh);
        content.addView(note("This standalone build keeps complaint data on the phone. Nearby alerts and status updates are local test functions; they are not shared with other staff devices until a backend/CMS API is connected."));
        scheduleReminder();
    }

    void showAllComplaints(){
        base("All Complaints");
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
        EditText search=field("Search customer, phone or issue"); top.addView(search,new LinearLayout.LayoutParams(0,WRAP,2));
        Spinner filter=new Spinner(this); String[] fs={"All","New","Assigned","In Progress","Resolved"}; filter.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,fs)); top.addView(filter,new LinearLayout.LayoutParams(0,WRAP,1));
        content.addView(top);

        LinearLayout listBox=new LinearLayout(this); listBox.setOrientation(LinearLayout.VERTICAL); content.addView(listBox);
        Runnable refresh=()->{
            listBox.removeAllViews();
            String q=search.getText().toString().trim().toLowerCase();
            String st=filter.getSelectedItem().toString();
            List<Ticket> list=db.getAllTickets();
            int shown=0;
            for(Ticket t:list){
                boolean matchStatus=st.equals("All")||t.status.equals(st);
                boolean matchText=q.isEmpty()||t.name.toLowerCase().contains(q)||t.phone.contains(q)||t.category.toLowerCase().contains(q)||t.message.toLowerCase().contains(q);
                if(matchStatus&&matchText){listBox.addView(ticketCard(t,true));shown++;}
            }
            if(shown==0)listBox.addView(note("No complaints match the current search/filter."));
        };
        Button apply=button("Search / Refresh List",BLUE); apply.setOnClickListener(v->refresh.run()); content.addView(apply);
        refresh.run();

        Button back=button("Back to Staff Dashboard",Color.GRAY); back.setOnClickListener(v->staffDashboard()); content.addView(back);
    }

    void showNearbyComplaints(){
        base("Nearby Complaints");
        content.addView(note("Radius: "+radiusKm+" km. Staff location must be updated first."));
        Button update=button("Update My Location",BLUE); update.setOnClickListener(v->getStaffLocationAndRefresh()); content.addView(update);
        nearbyList=new LinearLayout(this); nearbyList.setOrientation(LinearLayout.VERTICAL); content.addView(nearbyList);
        loadNearby();
        Button back=button("Back",Color.GRAY); back.setOnClickListener(v->staffDashboard()); content.addView(back);
    }

    void showCustomerSearch(){
        base("Customer Search");
        EditText phone=field("Customer mobile number"); content.addView(phone);
        Button find=button("Find Customer",BLUE); content.addView(find);
        LinearLayout result=new LinearLayout(this); result.setOrientation(LinearLayout.VERTICAL); content.addView(result);
        find.setOnClickListener(v->{
            result.removeAllViews();
            String p=phone.getText().toString().trim();
            String n=db.getUserName(p);
            if(n==null){result.addView(note("No local customer account was found for "+p+"."));return;}
            result.addView(card("Customer found"));
            result.addView(info("Name",n));
            result.addView(info("Mobile",p));
            result.addView(info("Customer ID",db.getCustomerId(p)));
            result.addView(info("Plan","100 Mbps"));
            result.addView(info("Service Status","Active"));
            result.addView(sectionTitle("Customer complaints"));
            List<Ticket> tickets=db.getCustomerTickets(p);
            if(tickets.isEmpty()) result.addView(note("No complaints."));
            else for(Ticket t:tickets) result.addView(ticketCard(t,true));
        });
        Button back=button("Back",Color.GRAY); back.setOnClickListener(v->staffDashboard()); content.addView(back);
    }

    void loadNearby(){
        if(nearbyList==null)return;
        nearbyList.removeAllViews();
        List<Ticket> list=db.getOpenTickets();
        boolean found=false;
        if(Double.isNaN(myLat)||Double.isNaN(myLng)){
            nearbyList.addView(note("Share your staff location first to see nearby complaints."));
            return;
        }
        for(Ticket t:list){
            if(Double.isNaN(t.lat)||Double.isNaN(t.lng))continue;
            double d=distanceKm(myLat,myLng,t.lat,t.lng);
            if(!Double.isNaN(d) && d<=radiusKm){
                found=true; nearbyList.addView(ticketCard(t,true));
            }
        }
        if(!found) nearbyList.addView(note("No open complaints with customer locations were found within "+radiusKm+" km."));
    }

    View ticketCard(Ticket t, boolean staff){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(16,16,16,16); box.setBackgroundColor(Color.WHITE);
        TextView h=text("#"+t.id+"  "+t.category,18,NAVY); h.setTypeface(null,Typeface.BOLD); box.addView(h);
        box.addView(info("Customer",t.name+" · "+t.phone));
        box.addView(info("Status",t.status+(t.assigned==null||t.assigned.isEmpty()?"":" · "+t.assigned)));
        box.addView(info("Issue",t.message));
        if(!Double.isNaN(t.lat)) box.addView(info("Location",String.format(Locale.US,"%.5f, %.5f",t.lat,t.lng)));
        double d=distanceKm(myLat,myLng,t.lat,t.lng); if(!Double.isNaN(d)) box.addView(info("Distance",String.format(Locale.US,"%.2f km",d)));

        box.setOnClickListener(v->showTicketDetails(t.id,staff));

        if(staff){
            LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
            Button open=button("Open Ticket",BLUE); open.setOnClickListener(v->showTicketDetails(t.id,true)); actions.addView(open,new LinearLayout.LayoutParams(0,WRAP,1));
            Button call=button("Call",GREEN); call.setOnClickListener(v->dial(t.phone)); actions.addView(call,new LinearLayout.LayoutParams(0,WRAP,1));
            if(!Double.isNaN(t.lat)){Button map=button("Map",Color.DKGRAY);map.setOnClickListener(v->openMap(t.lat,t.lng));actions.addView(map,new LinearLayout.LayoutParams(0,WRAP,1));}
            box.addView(actions);
        }
        box.setLayoutParams(new LinearLayout.LayoutParams(MATCH,WRAP));
        ((LinearLayout.LayoutParams)box.getLayoutParams()).setMargins(0,0,0,12);
        return box;
    }

    void showTicketDetails(long ticketId, boolean staff){
        Ticket t=db.getTicket(ticketId);
        if(t==null){toast("Ticket no longer exists");return;}
        base("Ticket #"+t.id);
        content.addView(card("#"+t.id+" · "+t.category));
        content.addView(info("Customer",t.name));
        content.addView(info("Phone",t.phone));
        content.addView(info("Status",t.status));
        content.addView(info("Assigned",t.assigned==null||t.assigned.isEmpty()?"Unassigned":t.assigned));
        content.addView(info("Source",t.source));
        content.addView(info("Issue",t.message));
        if(!Double.isNaN(t.lat))content.addView(info("Customer GPS",String.format(Locale.US,"%.6f, %.6f",t.lat,t.lng)));

        sectionTitle("Ticket history");
        List<String> notes=db.getTicketNotes(ticketId);
        if(notes.isEmpty())content.addView(note("No staff updates yet."));
        else for(String n:notes)content.addView(note(n));

        if(staff){
            sectionTitle("Staff actions");
            if(t.assigned==null||t.assigned.isEmpty()){
                Button claim=button("Claim Ticket",BLUE);claim.setOnClickListener(v->{db.assignTicket(t.id,currentName);db.addNote(t.id,currentName+" claimed this ticket.");showTicketDetails(t.id,true);});content.addView(claim);
            }
            String[] statuses={"New","Assigned","In Progress","Resolved","Closed"};
            Spinner status=new Spinner(this);status.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,statuses));
            for(int i=0;i<statuses.length;i++)if(statuses[i].equals(t.status))status.setSelection(i);
            content.addView(label("Change status"));content.addView(status);
            EditText note=multi("Add staff note");content.addView(note);
            Button save=button("Save Status / Note",GREEN);save.setOnClickListener(v->{String ns=status.getSelectedItem().toString();db.updateStatus(t.id,ns,currentName);String nv=note.getText().toString().trim();if(!nv.isEmpty())db.addNote(t.id,currentName+": "+nv);notifyUser("Ticket updated","#"+t.id+" is now "+ns);showTicketDetails(t.id,true);});content.addView(save);
            if(!Double.isNaN(t.lat)){Button map=button("Open Customer Location",Color.DKGRAY);map.setOnClickListener(v->openMap(t.lat,t.lng));content.addView(map);}
            Button call=button("Call Customer",BLUE);call.setOnClickListener(v->dial(t.phone));content.addView(call);
        }

        Button back=button(staff?"Back to Complaints":"Back to My Complaints",Color.GRAY);
        back.setOnClickListener(v->{if(staff)showAllComplaints();else showMyTickets();});
        content.addView(back);
    }

    void openMap(double lat,double lng){
        try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://maps.google.com/?q="+lat+","+lng)));}catch(Exception e){toast("Maps could not be opened");}
    }

    void dial(String number){
        try{startActivity(new Intent(Intent.ACTION_DIAL,android.net.Uri.parse("tel:"+number)));}catch(Exception e){toast("Call failed");}
    }

    void staffCreateTicket(){
        myLat=Double.NaN; myLng=Double.NaN;
        base("Helpdesk Complaint");
        EditText lookup=field("Customer mobile"); EditText msg=multi("Issue description");
        Spinner cat=new Spinner(this); String[] cats={"No Internet","Slow Speed","Frequent Disconnection","Router / ONU","Billing","Other"}; cat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));
        content.addView(lookup); content.addView(label("Issue type")); content.addView(cat); content.addView(msg);
        Button submit=button("Create Complaint",GREEN); submit.setOnClickListener(v->{String p=lookup.getText().toString().trim(),m=msg.getText().toString().trim();if(p.isEmpty()||m.isEmpty()){toast("Enter customer mobile and issue");return;}String n=db.getUserName(p);if(n==null)n="Customer";long id=db.addTicket(p,n,cat.getSelectedItem().toString(),m,myLat,myLng,"helpdesk_staff");notifyUser("Complaint created","#"+id+" created by helpdesk");staffDashboard();});content.addView(submit);
        Button back=button("← Back",Color.GRAY);back.setOnClickListener(v->staffDashboard());content.addView(back);
    }

    void runTroubleshooter(){
        base("Internet Troubleshooter");
        TextView out=text("Running diagnostics…",16,NAVY); content.addView(out);
        TextView tip=note("The test checks this phone's network and internet reachability. It cannot yet read live ONU/OLT/PPPoE state."); content.addView(tip);
        Button rerun=button("Run Again",BLUE); content.addView(rerun); rerun.setOnClickListener(v->runTroubleshooter());
        EXEC.execute(()->{
            boolean connected=isConnected();
            long ms=-1; boolean internet=false;
            if(connected){long st=System.currentTimeMillis();try{HttpURLConnection c=(HttpURLConnection)new URL("https://www.google.com/generate_204").openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.connect();internet=c.getResponseCode()>0;ms=System.currentTimeMillis()-st;c.disconnect();}catch(Exception ignored){}}
            final boolean cc=connected, ii=internet; final long mm=ms;
            runOnUiThread(()->{out.setText("Network: "+(cc?"CONNECTED":"NOT CONNECTED")+"\nInternet: "+(ii?"REACHABLE":"NOT REACHABLE")+"\nLatency to test endpoint: "+(mm>=0?mm+" ms":"—"));});
        });
    }

    boolean isConnected(){
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); if(Build.VERSION.SDK_INT>=23){NetworkCapabilities n=cm.getNetworkCapabilities(cm.getActiveNetwork());return n!=null&&(n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||n.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)||n.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));}return false;
    }

    void getStaffLocationAndRefresh(){ getLocation(true,null); }
    void getLocation(boolean staff,TextView status){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);toast("Allow location, then tap again.");return;}
        try{
            LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
            Location l=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); if(l==null)l=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(l==null){Intent i=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);startActivity(i);toast("Turn on Location, then tap again.");return;}
            myLat=l.getLatitude();myLng=l.getLongitude();
            if(status!=null){status.setText(String.format(Locale.US,"Location attached: %.5f, %.5f",myLat,myLng));status.setTextColor(GREEN);}
            else {toast("Staff location: "+String.format(Locale.US,"%.5f, %.5f",myLat,myLng));staffDashboard();}
        }catch(Exception e){toast("Could not read location. Turn Location on and try again.");}
    }

    void requestNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);
    }
    void notifyUser(String title,String body){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String ch="kn-alerts"; NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"Krishnanagar Net Alerts",NotificationManager.IMPORTANCE_HIGH));
        PendingIntent pi=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,ch).setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pi).build();
        nm.notify((int)(System.currentTimeMillis()%100000),n);
    }
    void scheduleReminder(){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);Intent i=new Intent(this,ReminderReceiver.class);PendingIntent p=PendingIntent.getBroadcast(this,77,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        long every=5*60*1000L; am.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+every,every,p);
    }

    void callSupport(){showSupport();}
    void clearSession(){currentPhone="";currentName="";role="";myLat=Double.NaN;myLng=Double.NaN;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("staffActive",false).apply();}
    double distanceKm(double a,double b,double c,double d){if(Double.isNaN(a)||Double.isNaN(b)||Double.isNaN(c)||Double.isNaN(d))return Double.NaN;float[] r=new float[1];Location.distanceBetween(a,b,c,d,r);return r[0]/1000.0;}

    void seedStaff(){db.ensureStaff("9811940740","Krishnanagar Staff",hash("Staff@1234"));}

    TextView text(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setPadding(0,6,0,6);return t;}
    TextView label(String s){TextView t=text(s,14,NAVY);t.setPadding(0,14,0,6);return t;}
    TextView note(String s){TextView t=text(s,13,Color.rgb(100,115,141));t.setPadding(0,12,0,12);return t;}
    TextView sectionTitle(String s){TextView t=text(s,16,NAVY);t.setTypeface(null,Typeface.BOLD);t.setPadding(0,18,0,8);return t;}
    TextView cardLabel(String s){TextView t=text(s,15,NAVY);t.setTypeface(null,Typeface.BOLD);return t;}
    View card(String s){TextView t=text(s,20,NAVY);t.setTypeface(null,Typeface.BOLD);t.setBackgroundColor(Color.WHITE);t.setPadding(16,16,16,16);t.setLayoutParams(margin());return t;}
    View statBox(String k,String v,int c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(12,12,12,12);b.setBackgroundColor(Color.WHITE);TextView a=text(k,12,Color.GRAY);TextView n=text(v,22,c);n.setTypeface(null,Typeface.BOLD);b.addView(a);b.addView(n);LinearLayout.LayoutParams p=margin();p.setMargins(0,0,8,10);b.setLayoutParams(p);return b;}
    View info(String k,String v){TextView t=text(k+": "+v,14,Color.rgb(64,81,110));t.setBackgroundColor(Color.WHITE);t.setPadding(14,10,14,10);t.setLayoutParams(margin());return t;}
    EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine();e.setPadding(14,12,14,12);e.setBackgroundColor(Color.WHITE);e.setLayoutParams(margin());return e;}
    EditText multi(String hint){EditText e=field(hint);e.setSingleLine(false);e.setMinLines(4);return e;}
    Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundColor(c);b.setPadding(12,6,12,6);b.setLayoutParams(margin());return b;}
    View spacer(int h){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(1,h));return s;}
    LinearLayout.LayoutParams margin(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(MATCH,WRAP);p.setMargins(0,0,0,10);return p;}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    static final int MATCH=-1,WRAP=-2;

    static String hash(String x){try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=d.digest(x.getBytes());StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format("%02x",v));return s.toString();}catch(Exception e){return x;}}

    static class Ticket { long id;String phone,name,category,message,status,assigned,source;double lat,lng;Ticket(long i,String p,String n,String c,String m,String st,String as,String src,double la,double lo){id=i;phone=p;name=n;category=c;message=m;status=st;assigned=as;source=src;lat=la;lng=lo;}}

    static class Db extends android.database.sqlite.SQLiteOpenHelper{
        Db(Context c){super(c,"kn.db",null,2);}
        public void onCreate(android.database.sqlite.SQLiteDatabase d){d.execSQL("CREATE TABLE users(customer_id TEXT PRIMARY KEY,name TEXT,phone TEXT UNIQUE,password TEXT);");d.execSQL("CREATE TABLE staff(phone TEXT PRIMARY KEY,name TEXT,password TEXT);");d.execSQL("CREATE TABLE tickets(id INTEGER PRIMARY KEY AUTOINCREMENT,phone TEXT,name TEXT,category TEXT,message TEXT,lat REAL,lng REAL,status TEXT,assigned TEXT,source TEXT,created INTEGER);");d.execSQL("CREATE TABLE ticket_notes(id INTEGER PRIMARY KEY AUTOINCREMENT,ticket_id INTEGER,note TEXT,created INTEGER);");}
        public void onUpgrade(android.database.sqlite.SQLiteDatabase d,int a,int b){if(a<2)d.execSQL("CREATE TABLE IF NOT EXISTS ticket_notes(id INTEGER PRIMARY KEY AUTOINCREMENT,ticket_id INTEGER,note TEXT,created INTEGER);");}
        void ensureStaff(String p,String n,String pw){android.database.sqlite.SQLiteDatabase d=getWritableDatabase();android.content.ContentValues v=new android.content.ContentValues();v.put("phone",p);v.put("name",n);v.put("password",pw);d.insertWithOnConflict("staff",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);}
        boolean userExists(String p){android.database.Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM users WHERE phone=?",new String[]{p});boolean x=c.moveToFirst();c.close();return x;}
        String createUser(String n,String p,String pw){String id="KN-"+String.format(Locale.US,"%05d",Math.abs(p.hashCode())%100000);android.content.ContentValues v=new android.content.ContentValues();v.put("customer_id",id);v.put("name",n);v.put("phone",p);v.put("password",pw);getWritableDatabase().insert("users",null,v);return id;}
        String getUserPassword(String p){android.database.Cursor c=getReadableDatabase().rawQuery("SELECT password FROM users WHERE phone=?",new String[]{p});String x=c.moveToFirst()?c.getString(0):null;c.close();return x;}
        String getUserName(String p){android.database.Cursor c=getReadableDatabase().rawQuery("SELECT name FROM users WHERE phone=?",new String[]{p});String x=c.moveToFirst()?c.getString(0):null;c.close();return x;}
        String getCustomerId(String p){android.database.Cursor c=getReadableDatabase().rawQuery("SELECT customer_id FROM users WHERE phone=?",new String[]{p});String x=c.moveToFirst()?c.getString(0):"—";c.close();return x;}
        long addTicket(String p,String n,String c,String m,double la,double lo,String src){android.content.ContentValues v=new android.content.ContentValues();v.put("phone",p);v.put("name",n);v.put("category",c);v.put("message",m);v.put("lat",la);v.put("lng",lo);v.put("status","New");v.put("assigned","");v.put("source",src);v.put("created",System.currentTimeMillis());return getWritableDatabase().insert("tickets",null,v);}
        void assignTicket(long id,String staff){android.content.ContentValues v=new android.content.ContentValues();v.put("assigned",staff);v.put("status","Assigned");getWritableDatabase().update("tickets",v,"id=?",new String[]{String.valueOf(id)});}
        void updateStatus(long id,String st,String staff){android.content.ContentValues v=new android.content.ContentValues();v.put("status",st);v.put("assigned",staff);getWritableDatabase().update("tickets",v,"id=?",new String[]{String.valueOf(id)});}
        List<Ticket> getCustomerTickets(String p){return query("SELECT * FROM tickets WHERE phone=? ORDER BY id DESC",new String[]{p});}
        List<Ticket> getOpenTickets(){return query("SELECT * FROM tickets WHERE status NOT IN ('Resolved','Closed') ORDER BY id DESC",new String[]{});}
        List<Ticket> query(String q,String[]args){ArrayList<Ticket> a=new ArrayList<>();android.database.Cursor c=getReadableDatabase().rawQuery(q,args);while(c.moveToNext()){a.add(new Ticket(c.getLong(c.getColumnIndexOrThrow("id")),c.getString(c.getColumnIndexOrThrow("phone")),c.getString(c.getColumnIndexOrThrow("name")),c.getString(c.getColumnIndexOrThrow("category")),c.getString(c.getColumnIndexOrThrow("message")),c.getString(c.getColumnIndexOrThrow("status")),c.getString(c.getColumnIndexOrThrow("assigned")),c.getString(c.getColumnIndexOrThrow("source")),c.getDouble(c.getColumnIndexOrThrow("lat")),c.getDouble(c.getColumnIndexOrThrow("lng"))));}c.close();return a;}
    }
}
