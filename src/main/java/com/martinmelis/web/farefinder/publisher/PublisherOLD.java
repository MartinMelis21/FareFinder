package com.martinmelis.web.farefinder.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.bican.wordpress.CustomField;
import net.bican.wordpress.FilterPost;
import net.bican.wordpress.MediaItem;
import net.bican.wordpress.MediaItemUploadResult;
import net.bican.wordpress.Post;
import net.bican.wordpress.Term;
import net.bican.wordpress.Wordpress;
import net.bican.wordpress.exceptions.FileUploadException;
import net.bican.wordpress.exceptions.InsufficientRightsException;
import net.bican.wordpress.exceptions.InvalidArgumentsException;
import net.bican.wordpress.exceptions.ObjectNotFoundException;
import redstone.xmlrpc.XmlRpcFault;

public class PublisherOLD {

	Wordpress wp = null;
	
	public void publishArticle (String origin, String destination, Integer price, Date outbound, Date inbound) throws Exception
	{
		wp = new Wordpress("martinmelis", "P(qo#zKmm6hfXAq*X8", "http://errorflights-martinmelis.rhcloud.com/xmlrpc.php");
		
		final FilterPost filter = new FilterPost();
	    filter.setNumber(10);
	    final List<Post> recentPosts = wp.getPosts(filter);
	    System.out.println("Here are the ten recent posts:");
	    for (final Post page : recentPosts) {
	      System.out.println(page.getPost_id() + ":" + page.getPost_title() + "\t");
	}
		
		
		System.out.println("Posting a a new fare...");
		Post recentPost = new Post();
		recentPost.setPost_title("Test to Test for 239€");
		recentPost.setPost_content("Outbound:	18.12.2016\nInbound 27.12.2016\nBook here:");
		
		
		Integer termId = null;
		final List<Term> terms = wp.getTerms("category");
		for (final Term t : terms) {
	        if (t.getName().equals("TopDeals")) {
	          termId = t.getTerm_id();
	          break;
	        }}
		
		MediaItem generic = null;
	    
	    for (MediaItem m : wp.getMediaLibrary())
	    {
	    	if(m.getTitle().equals("generic"))
	    	{
	    		System.out.println(m.getLink());
	    		generic = m;
	    		break;
	    	}
	    }
		
	    generic.setAttachment_id(18);
	    	    
		final Term term1 = wp.getTerm("category", termId);		
		recentPost.setTerms(Arrays.asList(new Term[] { term1}));
		recentPost.setPost_status("draft");
		recentPost.setPost_thumbnail(wp.getMediaItem(generic.getAttachment_id()));
		Integer postID = wp.newPost(recentPost);
		
		wp.editPost(18, recentPost);
		final List<CustomField> customFields = new ArrayList<>();
	    final CustomField cf1 = new CustomField();
	    cf1.setKey("thumb");
	    cf1.setValue("https://images.trvl-media.com/media/content/expus/graphics/launch/flight1320x742.jpg");
	    customFields.add(cf1);
	    recentPost.setCustom_fields(customFields);
	    
	   
		System.out.println("new post page id: " + postID);
	}
	
	
	
}
